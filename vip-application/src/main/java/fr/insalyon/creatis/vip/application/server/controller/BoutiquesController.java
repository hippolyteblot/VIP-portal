package fr.insalyon.creatis.vip.application.server.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.insalyon.creatis.vip.application.server.business.BoutiquesBusiness;
import fr.insalyon.creatis.vip.core.client.VipException;

@RestController
@RequestMapping("/boutiques")
public class BoutiquesController {

    private final BoutiquesBusiness boutiquesBusiness;

    @Autowired
    public BoutiquesController(BoutiquesBusiness boutiquesBusiness) {
        this.boutiquesBusiness = boutiquesBusiness;
    }

    @PostMapping(value = "check")
    public void check(@RequestBody String descriptorJson) throws VipException {
        String md5 = DigestUtils.md5Hex(descriptorJson);

        try {
            // JSON parsing to extract the "descriptor" field content
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> descriptorWrapper = mapper.readValue(descriptorJson, new TypeReference<Map<String, String>>() {});
            String descriptorContent = descriptorWrapper.get("descriptor");

            // Write in a temp file to validate with the descriptor
            Path tempFile = Files.createTempFile(md5, ".json");
            Files.write(tempFile, descriptorContent.getBytes(StandardCharsets.UTF_8));
            tempFile.toFile().deleteOnExit();
            System.out.println("Descriptor content written to temporary file: " + Path.of(tempFile.toString()).toString());
            boutiquesBusiness.validateBoutiquesFile(Path.of(tempFile.toString()).toString());
        } catch (IOException e) {
            throw new VipException("Error while saving descriptor: " + e.getMessage());
        }
    }

}
