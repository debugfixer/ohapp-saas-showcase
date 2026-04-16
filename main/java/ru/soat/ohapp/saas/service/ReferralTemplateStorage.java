package ru.soat.ohapp.saas.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Хранилище .docx-шаблонов направлений.
 * Поддерживает 2 "ключа" шаблонов:
 *  - PERIODIC     (для PERIODIC_1Y/2Y)
 *  - PSYCHIATRIC  (для псих. освидетельствования)
 *
 * Имя по умолчанию можно переопределить через application.yml,
 * но файлы лежат всегда в одной директории и выбираются по ключу.
 */
@Service
@Slf4j
public class ReferralTemplateStorage {

    private final Path dir;
    private final String defaultPeriodicName;
    private final String defaultPsychName;

    public ReferralTemplateStorage(
            @Value("${app.referrals.template-dir:templates/referrals}") String dir,
            @Value("${app.referrals.default-periodic:periodic.docx}") String defaultPeriodicName,
            @Value("${app.referrals.default-psychiatric:psychiatric.docx}") String defaultPsychName
    ) throws IOException {
        this.dir = Paths.get(dir).toAbsolutePath().normalize();
        this.defaultPeriodicName = defaultPeriodicName;
        this.defaultPsychName = defaultPsychName;
        Files.createDirectories(this.dir);
    }

    public enum Key { PERIODIC, PSYCHIATRIC }

    public Path save(MultipartFile file, Key key) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Файл шаблона пуст");
        String cleanName = sanitize(file.getOriginalFilename());
        if (!cleanName.toLowerCase().endsWith(".docx")) {
            throw new IllegalArgumentException("Поддерживаются только .docx");
        }
        Path uploaded = dir.resolve(cleanName);
        Files.copy(file.getInputStream(), uploaded, StandardCopyOption.REPLACE_EXISTING);

        // Сразу положим как "дефолтный" для выбранного ключа
        Path defTarget = getDefaultPath(key);
        Files.copy(uploaded, defTarget, StandardCopyOption.REPLACE_EXISTING);
        return defTarget;
    }

    public byte[] loadDefault(Key key) throws IOException {
        Path p = getDefaultPath(key);
        if (!Files.exists(p)) throw new IllegalArgumentException("Шаблон не загружен для ключа: " + key);
        return Files.readAllBytes(p);
    }

    public boolean defaultExists(Key key) {
        return Files.exists(getDefaultPath(key));
    }

    public String getDefaultName(Key key) {
        return (key == Key.PERIODIC) ? defaultPeriodicName : defaultPsychName;
    }

    public Path getDefaultPath(Key key) {
        return dir.resolve(getDefaultName(key));
    }

    public List<String> list() throws IOException {
        try (var s = Files.list(dir)) {
            return s.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.toLowerCase().endsWith(".docx"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    public void deleteAll() throws IOException {
        FileSystemUtils.deleteRecursively(dir);
        Files.createDirectories(dir);
    }

    private String sanitize(String n) {
        if (n == null) n = "template.docx";
        return n.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
