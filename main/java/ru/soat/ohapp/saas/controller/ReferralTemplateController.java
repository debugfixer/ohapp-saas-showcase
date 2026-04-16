package ru.soat.ohapp.saas.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.soat.ohapp.saas.service.ReferralTemplateStorage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/referrals/templates")
@RequiredArgsConstructor
public class ReferralTemplateController {

    private final ReferralTemplateStorage storage;

    /**
     * Загрузка шаблона с привязкой к "ключу".
     * key = PERIODIC | PSYCHIATRIC
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("key") ReferralTemplateStorage.Key key
    ) throws Exception {
        var path = storage.save(file, key);
        return ResponseEntity.ok(Map.of(
                "savedAs", path.getFileName().toString(),
                "key", key.name()
        ));
    }

    /** Имя дефолтного файла для каждого ключа */
    @GetMapping("/defaults")
    public Map<String, String> defaults() {
        return Map.of(
                "PERIODIC", storage.getDefaultName(ReferralTemplateStorage.Key.PERIODIC),
                "PSYCHIATRIC", storage.getDefaultName(ReferralTemplateStorage.Key.PSYCHIATRIC)
        );
    }

    /** Проверить наличие дефолта по ключу */
    @GetMapping("/exists/{key}")
    public Map<String, Boolean> exists(@PathVariable ReferralTemplateStorage.Key key) {
        return Map.of("exists", storage.defaultExists(key));
    }

    /** Список всех файлов в папке (диагностика) */
    @GetMapping
    public List<String> list() throws Exception {
        return storage.list();
    }

    /** Скачать дефолтный файл по ключу */
    @GetMapping("/download/{key}")
    public ResponseEntity<byte[]> downloadDefault(@PathVariable ReferralTemplateStorage.Key key) throws Exception {
        byte[] bytes = storage.loadDefault(key);
        String fn = URLEncoder.encode(storage.getDefaultName(key), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fn)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(bytes);
    }
}
