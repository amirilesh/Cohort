#report #sprint_2
Cohort — Sprint 2 Report

🔹 Context
In Sprint 2, the project evolved from a basic paper search tool into a working backend pipeline that retrieves full-text scientific papers and generates simplified study cards. The focus was on PDF retrieval, text extraction, and integrating an LLM for transforming complex research into understandable summaries.

🔹 Planned / Done
 • Implement full-text PDF retrieval pipeline from OpenAlex links [done]
 • Build PdfTextService for downloading and extracting text from PDFs [done]
 • Add support for redirects, cookies, and headers for better PDF access [done]
 • Implement PDF parsing using Apache PDFBox [done]
 • Add /pdftext endpoint for debugging and validating PDF extraction [done]
 • Implement /studycard endpoint for generating study cards from papers [done]
 • Integrate OpenAI API for LLM-based study card generation [done]
 • Design structured study card format (tldr, studyDesign, keyFindings, limitations) [done]
 • Improve prompt to generate simple, beginner-friendly explanations [done]
 • Add fallback mechanism when LLM fails (section-based parsing: abstract, results, etc.) [done]
 • Add “source” field to distinguish between LLM and fallback outputs [done]
 • Improve PDF retrieval robustness (headers, redirect handling, link discovery) [done]
 • Merge all backend work into main branch and clean up feature branches [done]

🔹 Demo
A working backend demo is available via local endpoints:
 • /search?q=... → returns open-access papers with PDF links
 • /pdftext?url=... → extracts raw text from a scientific paper
 • /studycard?url=... → generates a simplified study card from the paper

Example functionality:
 • User searches for a topic (e.g., “stress and sleep”)
 • System retrieves open-access papers
 • Backend extracts PDF text
 • LLM generates a simplified study card

🔹 Key Decisions
 • Introduced a full pipeline: search → PDF retrieval → text extraction → study card generation
 • Used Apache PDFBox for reliable PDF text extraction
 • Integrated OpenAI API for generating human-readable summaries
 • Added strict prompting to avoid hallucinations and enforce simple explanations
 • Implemented fallback logic to ensure system robustness when LLM fails
 • Kept architecture simple and modular (services: OpenAlexService, PdfTextService, StudyCardService)
 • Maintained AGENTS.md rules to prevent over-engineering and keep code readable

🔹 Problems & Next Steps
Problems:
 • PDF retrieval success rate is still inconsistent (~30–60% depending on publisher)
 • Some publishers block or complicate PDF access
 • LLM output quality depends heavily on prompt tuning
 • No persistent storage — all results are generated on demand

Next Steps (Sprint 3 focus):
 • Improve PDF retrieval success rate further (target ~70%+)
 • Introduce database (store papers, cache PDF status, cache study cards)
 • Implement validated search (only show papers that can be processed)
 • Start frontend development (UI with search + study cards + chat-like navigation)
 • Improve UX: allow users to browse multiple study cards and revisit previous queries