package com.minimemo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/memo")
@CrossOrigin(origins = "*")
public class MeetingController {

    private final ChatClient localOllamaChatClient;

    // Pattern matching common profanity, slurs, and abusive language (case-insensitive)
    private static final Pattern PROFANITY_PATTERN = Pattern.compile(
            "\\b(fuck|shit|bitch|asshole|bastard|dick|cunt|piss|damn|hell|crap|idiot|stupid)\\b",
            Pattern.CASE_INSENSITIVE
    );

    public MeetingController(ChatClient.Builder chatClientBuilder) {
        this.localOllamaChatClient = chatClientBuilder.build();
    }

    /**
     * Sanitizes raw text by masking abusive words with asterisks.
     */
    private String sanitizeProfanity(String input) {
        if (input == null) return "";
        return PROFANITY_PATTERN.matcher(input).replaceAll("****");
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateMemo(@RequestBody Map<String, String> payload) {
        try {
            String rawTranscript = payload.get("transcript");

            if (rawTranscript == null || rawTranscript.trim().length() < 10) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Transcript is too short or empty. Make sure CC is turned ON and people are speaking."
                ));
            }

            // Layer 1: Sanitize incoming transcript before the AI processes it
            String cleanTranscript = sanitizeProfanity(rawTranscript);

            // Layer 2: Strict prompt guardrails enforcing professional, respectful language
            String prompt = """
                You are MiniMemo, an executive-level meeting assistant.
                Summarize the following meeting transcript into a structured memo.

                STRICT WORKPLACE & SAFETY RULES:
                1. ZERO TOLERANCE FOR ABUSIVE LANGUAGE: Never include profanity, offensive remarks, slurs, or hostile attacks in the summary.
                2. If interpersonal arguments or abusive speech occurred, rephrase the core disagreement neutrally, professionally, and constructively (e.g., replace an insult with "Disagreement regarding project timelines").
                3. Completely IGNORE automated system messages ("cameras on", "joined the call", etc.).
                4. If the transcript contains only system text and no actual human discussion, reply EXACTLY with: "Waiting for actual conversation to begin..."

                Format requirements:
                # Executive Overview
                A 2-3 sentence overview of what has been discussed so far.

                # Key Highlights & Decisions
                - Bullet points covering major points and decisions made.

                # Current Action Items
                - List action items with assignees and explicit or implied deadlines (Mark TBD if unknown).

                Transcript:
                """ + cleanTranscript;

            String result = localOllamaChatClient.prompt(prompt).call().content();

            // Layer 3: Ensure the final output contains no leaked profanity before sending to PDF
            String safeOutput = sanitizeProfanity(result);

            return ResponseEntity.ok(Map.of("memo", safeOutput));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to generate memo: " + e.getMessage()
            ));
        }
    }
}