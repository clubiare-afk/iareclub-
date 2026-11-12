package com.iare.club.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudinaryStorageService {
    private static final Set<String> TYPES=Set.of("image/jpeg","image/png","image/webp","image/gif");
    private final Cloudinary cloudinary; private final boolean configured; private final long maxBytes; private final int maxFiles;
    private final ExecutorService uploadExecutor=Executors.newFixedThreadPool(3,r->{Thread thread=new Thread(r,"support-image-upload");thread.setDaemon(true);return thread;});
    public CloudinaryStorageService(@Value("${cloudinary.cloud-name:}")String cloud,
            @Value("${cloudinary.api-key:}")String key,@Value("${cloudinary.api-secret:}")String secret,
            @Value("${support.upload.max-file-size:5MB}")DataSize maxSize,@Value("${support.upload.max-files:5}")int maxFiles){
        configured=!cloud.isBlank()&&!key.isBlank()&&!secret.isBlank();maxBytes=maxSize.toBytes();this.maxFiles=maxFiles;
        cloudinary=new Cloudinary(ObjectUtils.asMap("cloud_name",cloud,"api_key",key,"api_secret",secret,"secure",true));
    }
    public List<Upload> upload(List<MultipartFile> files,String ticketNumber){
        List<MultipartFile> actual=files==null?List.of():files.stream().filter(f->f!=null&&!f.isEmpty()).toList();
        if(actual.size()>maxFiles)throw new UploadException("You can upload up to "+maxFiles+" images.");
        if(!actual.isEmpty()&&!configured)throw new UploadException("Support image uploads are temporarily unavailable. Please contact the administrator.");
        for(MultipartFile file:actual)validate(file);
        List<CompletableFuture<Upload>> tasks=actual.stream().map(file->CompletableFuture.supplyAsync(()->uploadOne(file,ticketNumber),uploadExecutor)).toList();
        try{CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();return tasks.stream().map(CompletableFuture::join).toList();}
        catch(CompletionException e){List<Upload> completed=tasks.stream().filter(CompletableFuture::isDone).filter(task->!task.isCompletedExceptionally()).map(CompletableFuture::join).toList();destroyQuietly(completed);Throwable cause=e.getCause();if(cause instanceof UploadException upload)throw upload;throw new UploadException("The images could not be uploaded. Please try again.");}
    }
    private void validate(MultipartFile file){String type=Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);if(!TYPES.contains(type))throw new UploadException("Only JPG, PNG, WEBP, and GIF images are allowed.");if(file.getSize()>maxBytes)throw new UploadException("Each image must be 5 MB or smaller.");}
    private Upload uploadOne(MultipartFile file,String ticketNumber){try{String type=file.getContentType().toLowerCase(Locale.ROOT);@SuppressWarnings("unchecked") Map<String,Object> result=cloudinary.uploader().upload(file.getBytes(),ObjectUtils.asMap("folder","fundrise/support/"+ticketNumber,"resource_type","image","type","upload"));return new Upload(result.get("secure_url").toString(),result.get("public_id").toString(),safeName(file.getOriginalFilename()),type);}catch(IOException|RuntimeException e){throw new UploadException("An image could not be uploaded. Please try again.");}}
    private String safeName(String name){String clean=Optional.ofNullable(name).orElse("image").replaceAll("[\\\\/\\r\\n]","_");return clean.length()>255?clean.substring(clean.length()-255):clean;}
    private void destroyQuietly(List<Upload> items){for(Upload item:items)try{cloudinary.uploader().destroy(item.publicId(),ObjectUtils.emptyMap());}catch(Exception ignored){}}
    public void discard(List<Upload> items){destroyQuietly(items);}
    @PreDestroy void shutdown(){uploadExecutor.shutdownNow();}
    public record Upload(String secureUrl,String publicId,String originalName,String contentType){}
    public static class UploadException extends RuntimeException{public UploadException(String message){super(message);}}
}
