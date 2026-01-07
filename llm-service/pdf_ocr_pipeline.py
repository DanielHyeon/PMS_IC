"""
PDF OCR pipeline for MinerU-based image/OCR extraction.
"""

import logging
import os
import shlex
import subprocess
import tempfile
from typing import List

from pdf2image import convert_from_path
from pypdf import PdfReader

logger = logging.getLogger(__name__)


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


def _run_mineru_cli_pdf(file_path: str) -> str:
    command = os.getenv("MINERU_OCR_COMMAND", "mineru").strip()
    if not command:
        raise RuntimeError("MINERU_OCR_COMMAND is required for mineru_cli OCR")
    method = os.getenv("MINERU_OCR_METHOD", "auto").strip() or "auto"
    backend = os.getenv("MINERU_OCR_BACKEND", "").strip()
    lang = os.getenv("MINERU_OCR_LANG", "").strip()

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


def extract_text_from_pdf(file_path: str) -> str:
    """
    Extract text from PDF, optionally using MinerU OCR pipeline.
    """
    use_ocr = os.getenv("USE_MINERU_OCR", "false").lower() == "true"
    if not use_ocr:
        return _extract_text_pypdf(file_path)

    try:
        return extract_text_from_pdf_ocr(file_path)
    except Exception as exc:
        logger.error("OCR extraction failed: %s", exc, exc_info=True)
        return _extract_text_pypdf(file_path)
