package com.csee.swplus.mileage.etcSubitem.controller;

import com.csee.swplus.mileage.etcSubitem.dto.EtcSubitemResponseDto;
import com.csee.swplus.mileage.etcSubitem.dto.StudentInputSubitemResponseDto;
import com.csee.swplus.mileage.etcSubitem.service.EtcSubitemService;
import com.csee.swplus.mileage.util.message.dto.MessageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController // 이 class 가 REST API 관련 class 라는 것을 스프링에게 명시
@RequestMapping("/api/mileage/etc")
@RequiredArgsConstructor
@Slf4j
public class EtcSubitemController {
    private final EtcSubitemService etcSubitemService;

//    학생이 증빙자료와 신청할 수 있게 관리자가 열어놓은 기타 항목 리스트 GET
    @GetMapping("")
    public ResponseEntity<List<StudentInputSubitemResponseDto>> getStudentInputSubitems () {
        return ResponseEntity.ok(
                etcSubitemService.getStudentInputSubitems()
        );
    }

//    학생이 신청한 기타 항목 리스트 GET
    @GetMapping("/get")
    public ResponseEntity<List<EtcSubitemResponseDto>> getEtcSubitems () {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(
                etcSubitemService.getEtcSubitems(currentUserId)
        );
    }

    @Value("${file.dir}")
    private String FILE_DIRECTORY;

    //    특정 기타 항목의 증빙 자료 다운로드
    @GetMapping("/file/{uniqueFileName}")
    public ResponseEntity<?> getEtcSubitemFile (@PathVariable String uniqueFileName) throws IOException {
        log.info("📂 Requested File: {}", uniqueFileName);

        Path path = Paths.get(FILE_DIRECTORY + uniqueFileName);
        log.info("🔍 Full File Path: {}", path.toString());

        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            log.error("❌ File not found or not readable: {}", path.toString());
            return ResponseEntity.notFound().build();
        }

        log.info("✅ File found: {}", path.toString());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + uniqueFileName + "\"")
                .body(resource);
    }

//    학생이 열려 있는 기타 항목 리스트 중 하나 선택 후 신청 POST
    @PostMapping(value = "/{studentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponseDto> postEtcSubitem (
            @PathVariable String studentId,
            @RequestParam("semester") String semester,
            @RequestParam(value = "description1", required = false) String description1,
            @RequestParam(value = "description2", required = false) String description2,
            @RequestParam("subitemId") int subitemId,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {

        if(file != null){
            String originalFilename = file.getOriginalFilename();

            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            }

            List<String> allowedExtenstions = Arrays.asList("pdf");

            if (!allowedExtenstions.contains(extension)) {
                return ResponseEntity.badRequest().body(new MessageResponseDto("지원하지 않는 파일 형식입니다."));
            }
        }

        return ResponseEntity.ok(
                etcSubitemService.postEtcSubitem(studentId, semester, description1, description2, subitemId, file)
        );
    }

//    기타 항목 수정
    @PatchMapping(value = "/{studentId}/{recordId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponseDto> patchEtcSubitem (
            @PathVariable String studentId,
            @PathVariable int recordId,
            @RequestParam(value = "description1", required = false) String description1,
            @RequestParam(value = "description2", required = false) String description2,
            @RequestParam("subitemId") int subitemId,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        if(file != null){
            String originalFilename = file.getOriginalFilename();

            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            }

            List<String> allowedExtenstions = Arrays.asList("pdf");

            if (!allowedExtenstions.contains(extension)) {
                return ResponseEntity.badRequest().body(new MessageResponseDto("지원하지 않는 파일 형식입니다."));
            }
        }

        return ResponseEntity.ok(
                etcSubitemService.patchEtcSubitem(studentId, recordId, description1, description2, subitemId, file)
        );
    }

//    기타 항목 삭제
    @DeleteMapping("/{studentId}/{recordId}")
    public ResponseEntity<MessageResponseDto> deleteEtcSubitem(
            @PathVariable String studentId,
            @PathVariable int recordId
    ) {
        return ResponseEntity.ok(
                etcSubitemService.deleteEtcSubitem(studentId, recordId)
        );
    }
}
