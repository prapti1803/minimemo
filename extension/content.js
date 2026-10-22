let seenCaptions = new Set();
let transcriptBuffer = [];

// Ignore standard UI button text and system notices
const systemUINoise = [
    "mic", "camera", "presenting", "hand raised", "leave call",
    "meeting details", "turn on", "turn off", "live captions are on",
    "font size", "loading", "your camera is off", "more options",
    "people", "chat with everyone", "jump to bottom"
];

function isRealSpeech(text) {
    if (!text || text.trim().length < 4) return false;
    const lower = text.trim().toLowerCase();

    // Exclude buttons or standard system strings
    if (systemUINoise.some(noise => lower === noise || lower.startsWith(noise))) return false;
    return true;
}

function scanBottomViewportCaptions() {
    // Captions always render in the bottom 30% of the browser viewport
    const bottomCutoff = window.innerHeight * 0.70;

    // Query all leaf text nodes in the DOM
    const elements = document.querySelectorAll('div, span, p');

    elements.forEach(el => {
        // Target only leaf nodes (nodes with no child elements containing text)
        if (el.children.length === 0 && el.innerText) {
            const rect = el.getBoundingClientRect();

            // Verify the element is visible and positioned near the bottom of the page
            if (rect.top >= bottomCutoff && rect.height > 0 && rect.width > 0) {
                const text = el.innerText.trim();

                if (isRealSpeech(text) && !seenCaptions.has(text)) {
                    seenCaptions.add(text);

                    const timestamp = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                    const entry = `[${timestamp}] ${text}`;
                    transcriptBuffer.push(entry);

                    console.log("📝 [MiniMemo Live Scraped]:", entry);

                    chrome.storage.local.set({
                        minimemo_transcript: transcriptBuffer.join("\n")
                    });
                }
            }
        }
    });
}

// Poll the bottom viewport every second
setInterval(scanBottomViewportCaptions, 1000);

// Also scan on DOM mutations
const observer = new MutationObserver(() => {
    scanBottomViewportCaptions();
});

observer.observe(document.body, {
    childList: true,
    subtree: true,
    characterData: true
});

console.log("🚀 MiniMemo Position-Based Caption Scraper Active!");