package com.campusflow.domain.portfolio.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class FileParserService {

    private static final int MAX_CHARS = 4000;

    public String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase() : "";

        if (filename.endsWith(".pdf")) {
            return extractFromPdf(file.getInputStream());
        } else if (filename.endsWith(".pptx")) {
            return extractFromPptx(file.getInputStream());
        } else {
            throw new IllegalArgumentException(
                    "지원하지 않는 파일 형식입니다. PDF, PPTX만 허용됩니다.");
        }
    }

    private String extractFromPdf(InputStream is) throws IOException {
        try (PDDocument doc = PDDocument.load(is)) {
            String text = new PDFTextStripper().getText(doc);
            return text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) + "..." : text;
        }
    }

    private String extractFromPptx(InputStream is) throws IOException {
        try (XMLSlideShow pptx = new XMLSlideShow(is)) {
            StringBuilder sb = new StringBuilder();
            for (var slide : pptx.getSlides()) {
                String slideName = slide.getSlideName();
                if (slideName != null && !slideName.isBlank()) {
                    sb.append("[").append(slideName).append("]\n");
                }
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (text != null && !text.isBlank()) {
                            sb.append(text.trim()).append("\n");
                        }
                    }
                }
                sb.append("\n");
                if (sb.length() > MAX_CHARS) break;
            }
            return sb.toString();
        }
    }
}
