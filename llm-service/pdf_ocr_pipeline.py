"""
PDF OCR pipeline for MinerU-based image/OCR extraction.

Includes:
- PDF type classification (Native vs Scanned vs Mixed)
- Smart Model Routing based on document complexity
- Optimized MinerU CLI integration
"""

import logging
import os
import shlex
import subprocess
import tempfile
from dataclasses import dataclass
from enum import Enum
from typing import Dict, List, Optional

from pdf2image import convert_from_path
from pypdf import PdfReader

try:
    import fitz  # PyMuPDF for advanced PDF analysis
    HAS_PYMUPDF = True
except ImportError:
    HAS_PYMUPDF = False

logger = logging.getLogger(__name__)


# =============================================================================
# PDF Type Classification (Section 4.3 of optimization document)
# =============================================================================

class PDFType(Enum):
    """PDF document type classification."""
    NATIVE = "native"    # Text-based PDF (no OCR needed)
    SCANNED = "scanned"  # Image-based PDF (OCR required)
    MIXED = "mixed"      # Hybrid document


def classify_pdf(pdf_path: str) -> PDFType:
    """
    Classify PDF type: Native vs Scanned vs Mixed.

    This helps optimize processing by:
    - Native: Use direct text extraction (faster, no OCR)
    - Scanned: Use OCR pipeline
    - Mixed: Use hybrid approach

    Args:
        pdf_path: Path to PDF file

    Returns:
        PDFType enum value
    """
    if HAS_PYMUPDF:
        return _classify_pdf_pymupdf(pdf_path)
    else:
        return _classify_pdf_pypdf(pdf_path)


def _classify_pdf_pymupdf(pdf_path: str) -> PDFType:
    """Classify PDF using PyMuPDF (more accurate)."""
    try:
        doc = fitz.open(pdf_path)
        text_pages = 0

        for page in doc:
            text = page.get_text()
            if len(text.strip()) > 100:
                text_pages += 1

        text_ratio = text_pages / len(doc) if len(doc) > 0 else 0
        doc.close()

        if text_ratio > 0.8:
            return PDFType.NATIVE
        elif text_ratio < 0.2:
            return PDFType.SCANNED
        else:
            return PDFType.MIXED

    except Exception as e:
        logger.warning("PyMuPDF classification failed: %s", e)
        return PDFType.MIXED


def _classify_pdf_pypdf(pdf_path: str) -> PDFType:
    """Fallback classification using pypdf."""
    try:
        reader = PdfReader(pdf_path)
        text_pages = 0

        for page in reader.pages:
            text = page.extract_text()
            if text and len(text.strip()) > 100:
                text_pages += 1

        text_ratio = text_pages / len(reader.pages) if len(reader.pages) > 0 else 0

        if text_ratio > 0.8:
            return PDFType.NATIVE
        elif text_ratio < 0.2:
            return PDFType.SCANNED
        else:
            return PDFType.MIXED

    except Exception as e:
        logger.warning("pypdf classification failed: %s", e)
        return PDFType.MIXED


# =============================================================================
# Smart Model Routing (Section 5.3 of optimization document)
# =============================================================================

class DocumentComplexity(Enum):
    """Document complexity level for model selection."""
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"


@dataclass
class DocumentAnalysis:
    """Analysis result for smart model routing."""
    complexity: DocumentComplexity
    has_tables: bool
    has_formulas: bool
    has_images: bool
    page_count: int
    pdf_type: PDFType
    recommended_backend: str
    recommended_config: Dict


class SmartModelRouter:
    """
    Document complexity analysis for optimal MinerU configuration.

    Analyzes PDF structure and recommends:
    - Backend selection (pipeline, hybrid, vlm)
    - Configuration optimizations
    """

    def __init__(self, sample_pages: int = 2):
        self.sample_pages = sample_pages

    def analyze_document(self, pdf_path: str) -> DocumentAnalysis:
        """
        Analyze document and recommend optimal MinerU configuration.

        Args:
            pdf_path: Path to PDF file

        Returns:
            DocumentAnalysis with recommendations
        """
        pdf_type = classify_pdf(pdf_path)

        if HAS_PYMUPDF:
            metrics = self._analyze_with_pymupdf(pdf_path)
        else:
            metrics = self._analyze_basic(pdf_path)

        complexity = self._calculate_complexity(metrics)
        recommendation = self._get_recommendation(complexity, metrics, pdf_type)

        return DocumentAnalysis(
            complexity=complexity,
            has_tables=metrics.get('table_indicators', 0) > 5,
            has_formulas=metrics.get('formula_indicators', 0) > 3,
            has_images=metrics.get('image_count', 0) > 2,
            page_count=metrics.get('page_count', 0),
            pdf_type=pdf_type,
            recommended_backend=recommendation['backend'],
            recommended_config=recommendation['config']
        )

    def _analyze_with_pymupdf(self, pdf_path: str) -> Dict:
        """Analyze PDF using PyMuPDF for detailed metrics."""
        try:
            doc = fitz.open(pdf_path)
            page_count = len(doc)
            sample_indices = self._get_sample_indices(page_count)

            metrics = {
                'page_count': page_count,
                'text_density': 0,
                'image_count': 0,
                'table_indicators': 0,
                'formula_indicators': 0,
                'sample_count': len(sample_indices)
            }

            for idx in sample_indices:
                page = doc[idx]

                # Text density
                text = page.get_text()
                metrics['text_density'] += len(text)

                # Image count
                images = page.get_images()
                metrics['image_count'] += len(images)

                # Table indicators (horizontal lines, grid patterns)
                drawings = page.get_drawings()
                horizontal_lines = sum(1 for d in drawings
                                      if self._is_horizontal_line(d))
                metrics['table_indicators'] += horizontal_lines

                # Formula indicators (special fonts, symbols)
                blocks = page.get_text("dict").get("blocks", [])
                for block in blocks:
                    if "lines" in block:
                        for line in block["lines"]:
                            for span in line.get("spans", []):
                                if self._has_math_font(span):
                                    metrics['formula_indicators'] += 1

            doc.close()
            return metrics

        except Exception as e:
            logger.warning("PyMuPDF analysis failed: %s", e)
            return self._analyze_basic(pdf_path)

    def _analyze_basic(self, pdf_path: str) -> Dict:
        """Basic analysis fallback using pypdf."""
        try:
            reader = PdfReader(pdf_path)
            page_count = len(reader.pages)

            return {
                'page_count': page_count,
                'text_density': sum(
                    len(p.extract_text() or '')
                    for p in reader.pages[:min(3, page_count)]
                ),
                'image_count': 0,
                'table_indicators': 0,
                'formula_indicators': 0,
                'sample_count': min(3, page_count)
            }
        except Exception as e:
            logger.warning("Basic analysis failed: %s", e)
            return {
                'page_count': 0,
                'text_density': 0,
                'image_count': 0,
                'table_indicators': 0,
                'formula_indicators': 0,
                'sample_count': 0
            }

    def _get_sample_indices(self, page_count: int) -> List[int]:
        """Determine sample page indices for analysis."""
        if page_count <= 2:
            return list(range(page_count))
        mid = page_count // 2
        return [mid - 1, mid] if page_count > 2 else [mid]

    def _is_horizontal_line(self, drawing: Dict) -> bool:
        """Check if drawing is a horizontal line (table indicator)."""
        return drawing.get('type') == 'l'

    def _has_math_font(self, span: Dict) -> bool:
        """Check if span uses mathematical font."""
        math_fonts = ['Symbol', 'MT Extra', 'Cambria Math', 'CMSY', 'CMMI']
        font = span.get('font', '')
        return any(mf.lower() in font.lower() for mf in math_fonts)

    def _calculate_complexity(self, metrics: Dict) -> DocumentComplexity:
        """Calculate document complexity from metrics."""
        sample_count = max(metrics.get('sample_count', 1), 1)

        avg_tables = metrics.get('table_indicators', 0) / sample_count
        avg_formulas = metrics.get('formula_indicators', 0) / sample_count
        avg_images = metrics.get('image_count', 0) / sample_count

        score = avg_tables * 2 + avg_formulas * 3 + avg_images * 1.5

        if score > 15:
            return DocumentComplexity.HIGH
        elif score > 5:
            return DocumentComplexity.MEDIUM
        else:
            return DocumentComplexity.LOW

    def _get_recommendation(
        self,
        complexity: DocumentComplexity,
        metrics: Dict,
        pdf_type: PDFType
    ) -> Dict:
        """Get recommended MinerU configuration based on analysis."""

        has_formulas = metrics.get('formula_indicators', 0) > 0
        has_tables = metrics.get('table_indicators', 0) > 0

        if complexity == DocumentComplexity.LOW:
            return {
                'backend': 'pipeline',
                'config': {
                    'layout-config': {'model': 'doclayout_yolo'},
                    'formula-config': {'enable': has_formulas},
                    'table-config': {
                        'model': 'rapid_table',
                        'enable': has_tables,
                        'max_time': 100
                    }
                }
            }

        elif complexity == DocumentComplexity.MEDIUM:
            return {
                'backend': 'hybrid-auto-engine',
                'config': {
                    'layout-config': {'model': 'doclayout_yolo'},
                    'formula-config': {'enable': True},
                    'table-config': {
                        'model': 'rapid_table',
                        'enable': True,
                        'max_time': 200
                    }
                }
            }

        else:  # HIGH complexity
            return {
                'backend': 'vlm-vllm' if pdf_type == PDFType.SCANNED else 'hybrid-auto-engine',
                'config': {
                    'layout-config': {'model': 'doclayout_yolo'},
                    'formula-config': {
                        'enable': True,
                        'mfr_model': 'unimernet_small'
                    },
                    'table-config': {
                        'model': 'rapid_table',
                        'sub_model': 'slanet_plus',
                        'enable': True,
                        'max_time': 400
                    }
                }
            }


# Global router instance
_smart_router: Optional[SmartModelRouter] = None


def get_smart_router() -> SmartModelRouter:
    """Get or create the smart model router singleton."""
    global _smart_router
    if _smart_router is None:
        _smart_router = SmartModelRouter()
    return _smart_router


# =============================================================================
# Original PDF extraction functions (updated)
# =============================================================================


def _extract_text_pypdf(file_path: str) -> str:
    reader = PdfReader(file_path)
    parts: List[str] = []
    for page in reader.pages:
        text = page.extract_text()
        if text:
            parts.append(text)
    return "\n\n".join(parts)


def _run_mineru_ocr_command(command: str, image_path: str) -> str:
    env = os.environ.copy()
    args = shlex.split(command) + [image_path]
    try:
        result = subprocess.run(
            args,
            capture_output=True,
            text=True,
            check=True,
            env=env,
        )
        return result.stdout.strip()
    except subprocess.CalledProcessError as exc:
        logger.error("MinerU OCR command failed: %s", " ".join(args))
        if exc.stdout:
            logger.error("MinerU OCR stdout: %s", exc.stdout.strip())
        if exc.stderr:
            logger.error("MinerU OCR stderr: %s", exc.stderr.strip())
        raise


def _run_mineru_cli_pdf(
    file_path: str,
    use_smart_routing: bool = True
) -> str:
    """
    Run MinerU CLI with optional smart routing.

    Smart routing analyzes the PDF and selects optimal backend/config
    based on document complexity and type.
    """
    command = os.getenv("MINERU_OCR_COMMAND", "mineru").strip()
    if not command:
        raise RuntimeError("MINERU_OCR_COMMAND is required for mineru_cli OCR")

    method = os.getenv("MINERU_OCR_METHOD", "auto").strip() or "auto"
    backend = os.getenv("MINERU_OCR_BACKEND", "").strip()
    lang = os.getenv("MINERU_OCR_LANG", "").strip()

    # Smart routing: analyze document and get recommended config
    analysis: Optional[DocumentAnalysis] = None
    if use_smart_routing and os.getenv("MINERU_SMART_ROUTING", "true").lower() == "true":
        try:
            router = get_smart_router()
            analysis = router.analyze_document(file_path)
            logger.info(
                "Smart routing: complexity=%s, pdf_type=%s, backend=%s",
                analysis.complexity.value,
                analysis.pdf_type.value,
                analysis.recommended_backend
            )

            # Use recommended backend if not explicitly set
            if not backend:
                backend = analysis.recommended_backend

            # Select method based on PDF type
            if method == "auto":
                if analysis.pdf_type == PDFType.NATIVE:
                    method = "txt"  # Direct text extraction
                elif analysis.pdf_type == PDFType.SCANNED:
                    method = "ocr"  # Full OCR
                else:
                    method = "auto"  # Let MinerU decide

        except Exception as e:
            logger.warning("Smart routing analysis failed: %s", e)

    extra_args = os.getenv("MINERU_OCR_ARGS", "").strip()
    args = shlex.split(command)
    if extra_args:
        args.extend(shlex.split(extra_args))

    with tempfile.TemporaryDirectory() as tmpdir:
        output_dir = os.path.join(tmpdir, "output")
        os.makedirs(output_dir, exist_ok=True)

        run_args = args + ["-p", file_path, "-o", output_dir, "-m", method]
        if backend and "-b" not in args and "--backend" not in args:
            run_args.extend(["-b", backend])
        if lang and "-l" not in args and "--lang" not in args:
            run_args.extend(["-l", lang])

        logger.info("Running MinerU CLI: %s", " ".join(run_args))

        try:
            subprocess.run(run_args, check=True, capture_output=True, text=True)
        except subprocess.CalledProcessError as exc:
            logger.error("MinerU CLI failed: %s", " ".join(run_args))
            if exc.stdout:
                logger.error("MinerU CLI stdout: %s", exc.stdout.strip())
            if exc.stderr:
                logger.error("MinerU CLI stderr: %s", exc.stderr.strip())
            raise

        candidates = []
        for root, _dirs, files in os.walk(output_dir):
            for name in files:
                if name.lower().endswith((".md", ".txt")):
                    candidates.append(os.path.join(root, name))

        if not candidates:
            return ""

        candidates.sort(key=lambda p: os.path.getmtime(p), reverse=True)
        with open(candidates[0], "r", encoding="utf-8") as handle:
            return handle.read().strip()


def _run_tesseract_ocr(image_path: str, lang: str) -> str:
    try:
        import pytesseract
    except ImportError as exc:
        raise RuntimeError("pytesseract is not installed") from exc

    return pytesseract.image_to_string(image_path, lang=lang).strip()


def extract_text_from_pdf_ocr(file_path: str) -> str:
    """
    Extract PDF text via image-based OCR pipeline.

    Env:
      USE_MINERU_OCR=true enables OCR pipeline
      MINERU_OCR_ENGINE=mineru|tesseract|pypdf
      MINERU_OCR_COMMAND="python3 /path/to/mineru_ocr.py"
      OCR_DPI=200
      OCR_LANG=kor+eng
    """
    ocr_engine = os.getenv("MINERU_OCR_ENGINE", "mineru_cli").lower()
    ocr_command = os.getenv("MINERU_OCR_COMMAND", "").strip()
    ocr_dpi = int(os.getenv("OCR_DPI", "200"))
    ocr_lang = os.getenv("OCR_LANG", "kor+eng")

    if ocr_engine == "pypdf":
        return _extract_text_pypdf(file_path)

    if ocr_engine == "mineru_cli":
        return _run_mineru_cli_pdf(file_path)

    images = convert_from_path(file_path, dpi=ocr_dpi)
    if not images:
        return ""

    parts: List[str] = []
    with tempfile.TemporaryDirectory() as tmpdir:
        for idx, image in enumerate(images, start=1):
            image_path = os.path.join(tmpdir, f"page_{idx}.png")
            image.save(image_path, "PNG")

            if ocr_engine == "mineru":
                if not ocr_command:
                    raise RuntimeError("MINERU_OCR_COMMAND is required for mineru OCR")
                page_text = _run_mineru_ocr_command(ocr_command, image_path)
            elif ocr_engine == "tesseract":
                page_text = _run_tesseract_ocr(image_path, ocr_lang)
            else:
                raise RuntimeError(f"Unknown OCR engine: {ocr_engine}")

            if page_text:
                parts.append(page_text)

    return "\n\n".join(parts)


def extract_text_from_pdf(file_path: str, auto_detect: bool = True) -> str:
    """
    Extract text from PDF with intelligent mode selection.

    Args:
        file_path: Path to PDF file
        auto_detect: If True, automatically detect PDF type and choose
                     optimal extraction method

    Behavior:
    - USE_MINERU_OCR=true: Always use OCR pipeline
    - USE_MINERU_OCR=false + auto_detect=True: Classify PDF and decide
    - USE_MINERU_OCR=false + auto_detect=False: Use pypdf only

    Environment variables:
    - USE_MINERU_OCR: Force OCR pipeline (true/false)
    - MINERU_SMART_ROUTING: Enable smart routing (true/false, default: true)
    - MINERU_AUTO_DETECT: Enable auto PDF type detection (true/false, default: true)
    """
    use_ocr = os.getenv("USE_MINERU_OCR", "false").lower() == "true"
    enable_auto_detect = os.getenv("MINERU_AUTO_DETECT", "true").lower() == "true"

    # If OCR is explicitly enabled, use it
    if use_ocr:
        try:
            return extract_text_from_pdf_ocr(file_path)
        except Exception as exc:
            logger.error("OCR extraction failed: %s", exc, exc_info=True)
            return _extract_text_pypdf(file_path)

    # Auto-detect PDF type and decide extraction method
    if auto_detect and enable_auto_detect:
        try:
            pdf_type = classify_pdf(file_path)
            logger.info("PDF type detected: %s for %s", pdf_type.value, file_path)

            if pdf_type == PDFType.NATIVE:
                # Native PDF - direct text extraction is sufficient
                text = _extract_text_pypdf(file_path)
                if text and len(text.strip()) > 100:
                    return text
                # Fallback to OCR if text extraction yields poor results
                logger.info("Native PDF text too short, trying OCR...")

            elif pdf_type == PDFType.SCANNED:
                # Scanned PDF - OCR is required
                logger.info("Scanned PDF detected, using OCR pipeline...")
                try:
                    return extract_text_from_pdf_ocr(file_path)
                except Exception as exc:
                    logger.warning("OCR failed for scanned PDF: %s", exc)

            else:  # MIXED
                # Try text extraction first, then OCR if needed
                text = _extract_text_pypdf(file_path)
                if text and len(text.strip()) > 500:
                    return text
                logger.info("Mixed PDF: text extraction insufficient, trying OCR...")
                try:
                    return extract_text_from_pdf_ocr(file_path)
                except Exception as exc:
                    logger.warning("OCR failed for mixed PDF: %s", exc)
                    return text  # Return whatever we got from pypdf

        except Exception as exc:
            logger.warning("Auto-detection failed: %s, falling back to pypdf", exc)

    # Default: simple pypdf extraction
    return _extract_text_pypdf(file_path)


def analyze_pdf(file_path: str) -> Dict:
    """
    Analyze PDF and return comprehensive document analysis.

    Useful for debugging and understanding document characteristics.

    Args:
        file_path: Path to PDF file

    Returns:
        Dict with analysis results including:
        - pdf_type: native/scanned/mixed
        - complexity: low/medium/high
        - has_tables, has_formulas, has_images
        - recommended_backend, recommended_config
    """
    router = get_smart_router()
    analysis = router.analyze_document(file_path)

    return {
        'file_path': file_path,
        'pdf_type': analysis.pdf_type.value,
        'complexity': analysis.complexity.value,
        'page_count': analysis.page_count,
        'has_tables': analysis.has_tables,
        'has_formulas': analysis.has_formulas,
        'has_images': analysis.has_images,
        'recommended_backend': analysis.recommended_backend,
        'recommended_config': analysis.recommended_config
    }
