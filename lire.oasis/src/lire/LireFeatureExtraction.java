//Caio Carneloz, 2018
package lire;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import net.semanticmetadata.lire.imageanalysis.features.GlobalFeature;
import net.semanticmetadata.lire.imageanalysis.features.global.CEDD;
import net.semanticmetadata.lire.imageanalysis.features.global.EdgeHistogram;
import net.semanticmetadata.lire.imageanalysis.features.global.FCTH;
import net.semanticmetadata.lire.imageanalysis.features.global.LocalBinaryPatterns;
import net.semanticmetadata.lire.imageanalysis.features.global.ScalableColor;
import net.semanticmetadata.lire.imageanalysis.features.global.centrist.SpatialPyramidCentrist;
import net.semanticmetadata.lire.utils.FileUtils;

public class LireFeatureExtraction 
{

    public static void main(String[] args) throws IOException
    {
        int i;
        String path = "";
        
        //Checa se o caminho passado existe e se é um diretório
        boolean passed = false;
        if (args.length > 0) 
        {
            path = args[0];
            File f = new File(path);
            System.out.println("% Indexing images in " + path);
            if (f.exists() && f.isDirectory()) passed = true;
        }
        if (!passed) 
        {
            System.out.println("No directory given as first argument.");
            System.out.println("Run \"CreateFile <directory>\" to index files of a directory.");
            System.exit(1);
        }
        
        extractFeatures(path);
    }
    
    private static void extractFeatures(String path) throws IOException
    {
        int i, j = 0;
        //Pega todas as imagens de um diretório e seus respectivos sub-diretórios
        ArrayList<String> images = FileUtils.getAllImages(new File(path), true);
        ArrayList labels = new ArrayList();
        
        //Cria as instâncias do LireFeature a serem utilizadas
        GlobalFeature feature = new EdgeHistogram();//ScalableColorDescriptor;FCTH;CEDD
        
        //Buscar classe no arquivo de texto com info clínica
        labels = getLabels(path);

        if (images.size()>0)
        {
            RandomAccessFile arq = new RandomAccessFile(path+"/EHD.csv","rw");
            
            //Obtém o número de dimensões
            feature.extract(ImageIO.read(new FileInputStream(images.get(0))));
            
            //Itera sobre as imagens extraindo as características
            for (Iterator<String> it = images.iterator(); it.hasNext();)
            {
                String imageFilePath = it.next();
                try 
                {
                    BufferedImage img = ImageIO.read(new FileInputStream(imageFilePath));
                    feature.extract(img);
                    for (i = 0; i < feature.getFeatureVector().length-1; i++) 
                    {
                        double v = feature.getFeatureVector()[i];
                        arq.writeBytes((Double.toString(v)).replace(',', '.') + ",");
                    }
                    double v = feature.getFeatureVector()[i];
                    arq.writeBytes((Double.toString(v)).replace(',', '.'));
                    arq.writeBytes(","+ labels.get(j));
                    
                    arq.writeBytes(System.getProperty("line.separator"));
                } 
                catch (Exception e) 
                {
                    System.err.println("Error reading image or indexing it.");
                    e.printStackTrace();
                }
                j++;
            }
            
            //closing the IndexWriter
            System.out.println("% Finished indexing.");
        }  
        else
            System.err.println("No images found in given directory: " + path);
    }
    
    private static ArrayList getLabels(String path) throws IOException
    {
        ArrayList labels = new ArrayList();
        String aux[];
        int i;
        int label;
        
        RandomAccessFile info;
        
        File f = new File(path);

        FilenameFilter textFilter = new FilenameFilter() 
        {
            @Override
            public boolean accept(File dir, String name) 
            {
                return name.toLowerCase().endsWith("mr1.txt");
            }
        };

        File[] files = f.listFiles(textFilter);
        for (File file : files) 
        {
            info = new RandomAccessFile(file.getAbsolutePath(), "r");
            
            for (i = 0; i < 6; i++)
                info.readLine();
            
            aux = info.readLine().split(":");
            
            if(!aux[1].equals("          "))
            {
                if(Double.parseDouble(aux[1]) > 0)
                    labels.add("Alzheimer");
                else
                    labels.add("Control");
            }
            else
                labels.add("Control");
        }
        
        return labels;
    }
}