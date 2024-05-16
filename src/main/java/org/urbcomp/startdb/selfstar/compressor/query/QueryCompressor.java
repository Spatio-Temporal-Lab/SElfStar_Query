package org.urbcomp.startdb.selfstar.compressor.query;

import org.urbcomp.startdb.selfstar.compressor.ICompressor;
import org.urbcomp.startdb.selfstar.utils.BlockReader;
import org.urbcomp.startdb.selfstar.query.CompressedBlock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class QueryCompressor implements IQueryCompressor{
    private static final String folderPath_Bytes_Chunk = "D:/bytes/ChunkBytes/";
    private ICompressor compressor;
    private String fileName;
    private CompressedBlock block;
    private int blockDataCapacity;
    private List<File> blockFiles = new ArrayList<>();

    public QueryCompressor(ICompressor compressor, String filename){
        this(compressor,filename,1024);
    }

    public QueryCompressor(ICompressor compressor, String filename, int blockdatabitsize){
        this.compressor = compressor;
        this.fileName = filename;
        this.block = new CompressedBlock();
        this.blockDataCapacity = blockdatabitsize * 8;
        chunk();
        // writeFilesToFile(blockFiles,fileName);
    }

    public void chunk(){
        int currentDataIndex = 0;
        long currentBitSize;
        double maxValue = Integer.MIN_VALUE;
        double minValue = Integer.MAX_VALUE;
        File blockFile;
        try (BlockReader br = new BlockReader(fileName,1000)){
            List<Double> floatings;
            boolean blockIfFull = true;
            while ((floatings = br.nextBlock()) != null){
                for (int i = 0; i < floatings.size(); i++){
                    double floating = floatings.get(i);
                    long beforeAddValueBitsSize = compressor.getCompressedSizeInBits();
                    compressor.addValue(floating);
                    currentBitSize = compressor.getCompressedSizeInBits();
                    if (blockIfFull || currentBitSize > blockDataCapacity){
                        //write data into current CompressedBlock
                        if (currentDataIndex != 0) {
                            //write data[] and WrittenBitSize
                            block.resetMinValue(minValue);
                            block.resetMaxValue(maxValue);
                            block.resetWrittenBitSize(beforeAddValueBitsSize);
                            block.resetData(compressor.getBytes(),beforeAddValueBitsSize);
                            blockFile = createFiles(block.getIData(),fileName);
                            block.writeToFile(blockFile);
                            blockFiles.add(blockFile);
                        }

                        //CompressedBlock refresh
                        block.refresh();
                        block.resetIData(currentDataIndex);
                        minValue = floating;
                        maxValue = floating;
                        compressor.refresh();
                        compressor.addValue(floating);
                        blockIfFull = false;
                    }
                    else {
                        if(floating > maxValue){
                            maxValue = floating;
                        }
                        else if(floating < minValue){
                            minValue = floating;
                        }

                        if(currentBitSize == blockDataCapacity){
                            blockIfFull = true;
                        }
                    }

                    currentDataIndex++;
                }
            }
            // write into the last part
            long beforeAddValueBitsSize = compressor.getCompressedSizeInBits();
            compressor.addValue(88.88888888) ;  //解决最后未byte为写满的问题
            block.resetMinValue(minValue);
            block.resetMaxValue(maxValue);
            block.resetWrittenBitSize(beforeAddValueBitsSize);
            block.resetData(compressor.getBytes(),beforeAddValueBitsSize);
            blockFile = createFiles(block.getIData(),fileName);
            block.writeToFile(blockFile);
            blockFiles.add(blockFile);

            //将第一个block的iData改为最后一个block的最后一个数据的id+1
            block.readFromFile(blockFiles.get(0));
            block.resetIData(currentDataIndex);
            block.writeToFile(blockFiles.get(0));

        }catch (Exception e) {
            throw new RuntimeException(fileName, e);
        }
    }

    public List<File> getBlockFiles(){
        return blockFiles;
    }

    // 将文件列表写入文件
    private void writeFilesToFile(List<File> blockFiles, String datasetFile) {
        File folder = new File(folderPath_Bytes_Chunk + datasetFile + "/");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File catalogFile = new File(folderPath_Bytes_Chunk + datasetFile + "/" + "blockFiles");
        try {
            if (!catalogFile.exists()){
                catalogFile.createNewFile();
            }
            else {
                boolean ifClear = clearFile(catalogFile);
                if (!ifClear){
                    System.out.println("Fail to clear the file" );
                }
            }
        } catch (IOException e) {
            System.out.println("Fail to create the file" );
            e.printStackTrace();
        }

        try (FileOutputStream fileOut = new FileOutputStream(catalogFile);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(blockFiles);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private File createFiles(int iData,String datasetFile) {
        File folder = new File(folderPath_Bytes_Chunk + datasetFile + "/");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        String fileName = iData + ".txt";
        File file = new File(folderPath_Bytes_Chunk + datasetFile + "/" + fileName);
        try {
            if (!file.exists()){
                file.createNewFile();
            }
            else {
                boolean ifClear = clearFile(file);
                if (!ifClear){
                    System.out.println("Fail to clear the file: " + fileName);
                }
            }
        } catch (IOException e) {
            System.out.println("Fail to create the file:" + fileName);
            e.printStackTrace();
        }

        return file;

    }

    private boolean clearFile(File file){
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // Do nothing, just opening the file in non-append mode will clear it
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file.length() == 0) {
            return true;
        } else {
            return false;
        }
    }
}
