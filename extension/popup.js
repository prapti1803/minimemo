document.addEventListener('DOMContentLoaded', () => {
    const btnGenerate = document.getElementById('btnGenerate');
    const btnPdf = document.getElementById('btnPdf');
    const btnClear = document.getElementById('btnClear');
    const outputDiv = document.getElementById('output');
    const statusText = document.getElementById('statusText');

    btnGenerate.addEventListener('click', async () => {
        outputDiv.innerText = "Summarizing live transcript so far...";
        btnGenerate.disabled = true;

        chrome.storage.local.get(["minimemo_transcript"], async (result) => {
            const transcript = result.minimemo_transcript || "";

            if (!transcript) {
                outputDiv.innerText = "No captions detected yet!\n\nPlease make sure Closed Captions (CC) are turned ON in your meeting tab.";
                btnGenerate.disabled = false;
                return;
            }

            try {
                const response = await fetch("http://localhost:8080/api/memo/generate", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ transcript: transcript })
                });

                const data = await response.json();

                if (data.memo) {
                    outputDiv.innerText = data.memo;
                    btnPdf.style.display = "block";
                    statusText.innerText = "Memo generated!";
                } else {
                    outputDiv.innerText = "Error: " + (data.error || "Failed to generate memo.");
                }
            } catch (err) {
                outputDiv.innerText = "Cannot connect to backend server. Ensure Spring Boot is running on localhost:8080.";
            } finally {
                btnGenerate.disabled = false;
            }
        });
    });

    // Native Print-to-PDF Window
    btnPdf.addEventListener('click', () => {
        const printWindow = window.open('', '_blank', 'width=800,height=600');
        const memoContent = outputDiv.innerText.replace(/\n/g, '<br>');

        printWindow.document.write(`
      <!DOCTYPE html>
      <html>
      <head>
        <title>MiniMemo Meeting Notes</title>
        <style>
          body { font-family: Arial, sans-serif; padding: 30px; line-height: 1.6; color: #222; }
          h1 { color: #1a73e8; border-bottom: 2px solid #1a73e8; padding-bottom: 8px; }
          .content { font-size: 14px; background: #f9f9f9; padding: 20px; border-radius: 8px; border: 1px solid #ddd; }
        </style>
      </head>
      <body>
        <h1>📝 MiniMemo - Meeting Summary</h1>
        <div class="content">${memoContent}</div>
        <script>
          window.onload = function() {
            window.print();
          };
        </script>
      </body>
      </html>
    `);
        printWindow.document.close();
    });

    btnClear.addEventListener('click', () => {
        chrome.storage.local.remove("minimemo_transcript", () => {
            outputDiv.innerText = "Transcript cleared! Ready for new meeting captions.";
            statusText.innerText = "Storage reset";
            btnPdf.style.display = "none";
        });
    });
});