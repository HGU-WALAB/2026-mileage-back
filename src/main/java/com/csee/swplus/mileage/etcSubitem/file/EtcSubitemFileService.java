package com.csee.swplus.mileage.etcSubitem.file;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption; // 파일 복사 관련 설정 제공 e.g. REPLACE_EXISTING
//import java.nio.file.attribute.PosixFilePermission;
//import java.nio.file.attribute.PosixFilePermissions;
//import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class EtcSubitemFileService {
    @Value("${file.upload-dir}")
    @Getter
    private String uploadDir;

    public String saveFile(MultipartFile file) throws IOException {
//        1. 업로드 디렉토리 생성
        File directory = new File(uploadDir);
        if (!directory.exists()) {
//            mkdir 와의 차이: mkdirs 는 만들고자 하는 디렉토리의 상위 디렉토리가 존재하지 않을 경우 상위 디렉토리까지 생성
            directory.mkdirs();
        }

//        2. 파일명 중복 방지를 위한 고유 파일명 생성 X -> 파일명 저장 형식 전달 받음
        String originalFilename = file.getOriginalFilename();
//        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")); // 파일 확장자 추출
//        String savedFilename = UUID.randomUUID().toString() + extension; // 고유한 파일명 생성 후 원본 확장자를 붙여 새로운 파일명 생성

//        3. 파일 저장
//        uploadDir 경로를 Path 객체로 변환
//        새 파일명과 결합하여 최종 저장 위치를 설정
        Path targetLocation = Paths.get(uploadDir).resolve(originalFilename);

//        file.getInputStream() 을 사용하여 업로드된 파일의 데이터를 읽음
//        targetLocation 경로에 복사
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation);
        }

//        try {
//            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-r—r—");
//            Files.setPosixFilePermissions(targetLocation, perms);
//        } catch (UnsupportedOperationException e) {
//            // Windows에서는 권한 변경을 무시하고 진행
//        }

//        4. 저장된 파일명 (새로 만들어진 고유의 파일명) 반환
        return originalFilename;
    }

    public void deleteFile(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("파일 삭제 중 오류 발생: " + filename, e);
            throw new RuntimeException("파일 삭제 중 오류가 발생했습니다.");
        }
    }

    public String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
