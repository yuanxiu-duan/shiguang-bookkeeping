# -*- coding: utf-8 -*-
"""
Markdown 转 Word 文档转换脚本
将 docs 目录下的 markdown 文档转换为中文命名的 docx 文件
"""
import os
import re
from docx import Document
from docx.shared import Pt, RGBColor, Inches, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

# 文件名映射：英文 md → 中文 docx
FILE_MAP = {
    'PRD_PRODUCT_REQUIREMENTS.md': '产品需求文档.docx',
    'DATABASE_DESIGN.md': '数据库设计文档.docx',
    'SYSTEM_ARCHITECTURE.md': '系统架构设计文档.docx',
    'API_SPECIFICATION.md': '接口文档.docx',
    'TEST_CASES.md': '测试用例文档.docx',
    'DEPLOYMENT_GUIDE.md': '部署运维手册.docx',
    'USER_MANUAL.md': '用户使用手册.docx',
    'DELIVERABLES.md': '项目交付清单.docx',
}

DOCS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'docs')


def set_cell_background(cell, color_hex):
    """设置表格单元格背景色"""
    shading = OxmlElement('w:shd')
    shading.set(qn('w:fill'), color_hex)
    shading.set(qn('w:val'), 'clear')
    cell._tc.get_or_add_tcPr().append(shading)


def set_run_font(run, font_name='微软雅黑', size=None, bold=None, color=None, italic=None):
    """设置 run 的字体（含中文东亚字体）"""
    run.font.name = font_name
    r = run._element
    rPr = r.get_or_add_rPr()
    rFonts = rPr.find(qn('w:rFonts'))
    if rFonts is None:
        rFonts = OxmlElement('w:rFonts')
        rPr.append(rFonts)
    rFonts.set(qn('w:eastAsia'), font_name)
    rFonts.set(qn('w:ascii'), font_name)
    rFonts.set(qn('w:hAnsi'), font_name)
    if size is not None:
        run.font.size = Pt(size)
    if bold is not None:
        run.font.bold = bold
    if italic is not None:
        run.font.italic = italic
    if color is not None:
        run.font.color.rgb = color


def add_inline_formatting(paragraph, text, base_size=10.5):
    """处理行内格式：**加粗**、*斜体*、`行内代码`、[链接](url)"""
    # 处理链接 [text](url) -> 只保留 text
    text = re.sub(r'\[([^\]]+)\]\([^\)]+\)', r'\1', text)
    
    # 使用正则分割行内格式
    pattern = r'(\*\*[^*]+\*\*|`[^`]+`|\*[^*]+\*|~~[^~]+~~)'
    parts = re.split(pattern, text)
    
    for part in parts:
        if not part:
            continue
        if part.startswith('**') and part.endswith('**'):
            run = paragraph.add_run(part[2:-2])
            set_run_font(run, size=base_size, bold=True)
        elif part.startswith('`') and part.endswith('`'):
            run = paragraph.add_run(part[1:-1])
            set_run_font(run, font_name='Consolas', size=base_size - 0.5, color=RGBColor(0xC7, 0x25, 0x4E))
        elif part.startswith('*') and part.endswith('*') and not part.startswith('**'):
            run = paragraph.add_run(part[1:-1])
            set_run_font(run, size=base_size, italic=True)
        elif part.startswith('~~') and part.endswith('~~'):
            run = paragraph.add_run(part[2:-2])
            set_run_font(run, size=base_size)
            run.font.strike = True
        else:
            run = paragraph.add_run(part)
            set_run_font(run, size=base_size)


def parse_table(lines, start_idx):
    """解析 markdown 表格，返回 (rows, next_idx)"""
    rows = []
    i = start_idx
    while i < len(lines) and '|' in lines[i] and lines[i].strip():
        line = lines[i].strip()
        if line.startswith('|'):
            line = line[1:]
        if line.endswith('|'):
            line = line[:-1]
        cells = [c.strip() for c in line.split('|')]
        rows.append(cells)
        i += 1
    # 跳过分隔行（第二行通常是 |---|---|）
    if len(rows) >= 2 and re.match(r'^[\s\-:|]+$', rows[1][0]) if rows[1] else False:
        # 检查是否是分隔行
        is_separator = all(re.match(r'^[\s\-:]+$', c) for c in rows[1])
        if is_separator:
            header = rows[0]
            data_rows = rows[2:]
        else:
            header = rows[0]
            data_rows = rows[1:]
    else:
        header = rows[0] if rows else []
        data_rows = rows[1:]
    return header, data_rows, i


def add_table_to_doc(doc, header, data_rows):
    """向文档添加表格"""
    if not header:
        return
    num_cols = len(header)
    num_rows = len(data_rows) + 1
    table = doc.add_table(rows=num_rows, cols=num_cols)
    table.style = 'Table Grid'
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    
    # 表头
    for j, cell_text in enumerate(header):
        if j < num_cols:
            cell = table.rows[0].cells[j]
            cell.text = ''
            p = cell.paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            add_inline_formatting(p, cell_text, base_size=10)
            set_cell_background(cell, '4472C4')
            for run in p.runs:
                run.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
                run.font.bold = True
    
    # 数据行
    for i, row in enumerate(data_rows):
        for j, cell_text in enumerate(row):
            if j < num_cols:
                cell = table.rows[i + 1].cells[j]
                cell.text = ''
                p = cell.paragraphs[0]
                add_inline_formatting(p, cell_text, base_size=10)
                # 隔行变色
                if i % 2 == 1:
                    set_cell_background(cell, 'D9E2F3')


def add_code_block(doc, code_lines, lang=''):
    """添加代码块"""
    p = doc.add_paragraph()
    p.paragraph_format.left_indent = Cm(0.5)
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(6)
    # 添加背景色通过段落边框实现（简化处理）
    for i, line in enumerate(code_lines):
        if i > 0:
            p.add_run('\n')
        run = p.add_run(line)
        set_run_font(run, font_name='Consolas', size=9, color=RGBColor(0x33, 0x33, 0x33))


def convert_md_to_docx(md_path, docx_path):
    """将单个 markdown 文件转换为 docx"""
    with open(md_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    lines = content.split('\n')
    
    doc = Document()
    
    # 设置默认样式
    style = doc.styles['Normal']
    style.font.name = '微软雅黑'
    style.font.size = Pt(10.5)
    style.element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')
    
    # 设置页边距
    for section in doc.sections:
        section.top_margin = Cm(2.5)
        section.bottom_margin = Cm(2.5)
        section.left_margin = Cm(2.5)
        section.right_margin = Cm(2.5)
    
    i = 0
    in_code_block = False
    code_lines = []
    code_lang = ''
    
    while i < len(lines):
        line = lines[i]
        
        # 代码块处理
        if line.strip().startswith('```'):
            if in_code_block:
                # 结束代码块
                add_code_block(doc, code_lines, code_lang)
                code_lines = []
                in_code_block = False
            else:
                # 开始代码块
                in_code_block = True
                code_lang = line.strip()[3:]
            i += 1
            continue
        
        if in_code_block:
            code_lines.append(line)
            i += 1
            continue
        
        stripped = line.strip()
        
        # 空行
        if not stripped:
            i += 1
            continue
        
        # 标题
        if stripped.startswith('#'):
            match = re.match(r'^(#{1,6})\s+(.*)', stripped)
            if match:
                level = len(match.group(1))
                title_text = match.group(2)
                # 移除标题中的 emoji 和特殊符号处理
                heading = doc.add_heading(level=level)
                run = heading.add_run(title_text)
                # 设置标题字体
                font_name = '微软雅黑'
                run.font.name = font_name
                rPr = run._element.get_or_add_rPr()
                rFonts = rPr.find(qn('w:rFonts'))
                if rFonts is None:
                    rFonts = OxmlElement('w:rFonts')
                    rPr.append(rFonts)
                rFonts.set(qn('w:eastAsia'), font_name)
                rFonts.set(qn('w:ascii'), font_name)
                rFonts.set(qn('w:hAnsi'), font_name)
                # 标题颜色
                if level == 1:
                    run.font.color.rgb = RGBColor(0x1F, 0x38, 0x64)
                    run.font.size = Pt(22)
                elif level == 2:
                    run.font.color.rgb = RGBColor(0x2E, 0x54, 0x96)
                    run.font.size = Pt(16)
                elif level == 3:
                    run.font.color.rgb = RGBColor(0x44, 0x72, 0xC4)
                    run.font.size = Pt(14)
                else:
                    run.font.color.rgb = RGBColor(0x44, 0x72, 0xC4)
                    run.font.size = Pt(12)
                i += 1
                continue
        
        # 分隔线
        if stripped == '---' or stripped == '***' or stripped == '___':
            p = doc.add_paragraph()
            p.paragraph_format.space_before = Pt(6)
            p.paragraph_format.space_after = Pt(6)
            # 添加水平线
            pPr = p._element.get_or_add_pPr()
            pBdr = OxmlElement('w:pBdr')
            bottom = OxmlElement('w:bottom')
            bottom.set(qn('w:val'), 'single')
            bottom.set(qn('w:sz'), '6')
            bottom.set(qn('w:space'), '1')
            bottom.set(qn('w:color'), '999999')
            pBdr.append(bottom)
            pPr.append(pBdr)
            i += 1
            continue
        
        # 引用块
        if stripped.startswith('>'):
            quote_text = stripped[1:].strip()
            p = doc.add_paragraph()
            p.paragraph_format.left_indent = Cm(1)
            p.paragraph_format.space_before = Pt(3)
            p.paragraph_format.space_after = Pt(3)
            # 左边框
            pPr = p._element.get_or_add_pPr()
            pBdr = OxmlElement('w:pBdr')
            left = OxmlElement('w:left')
            left.set(qn('w:val'), 'single')
            left.set(qn('w:sz'), '18')
            left.set(qn('w:space'), '8')
            left.set(qn('w:color'), '4472C4')
            pBdr.append(left)
            pPr.append(pBdr)
            add_inline_formatting(p, quote_text, base_size=10)
            for run in p.runs:
                run.font.color.rgb = RGBColor(0x66, 0x66, 0x66)
                run.font.italic = True
            i += 1
            continue
        
        # 表格检测
        if '|' in stripped and stripped.startswith('|'):
            header, data_rows, next_idx = parse_table(lines, i)
            if header:
                add_table_to_doc(doc, header, data_rows)
                doc.add_paragraph()  # 表格后空行
                i = next_idx
                continue
        
        # 有序列表
        ol_match = re.match(r'^(\d+)\.\s+(.*)', stripped)
        if ol_match:
            p = doc.add_paragraph(style='List Number')
            add_inline_formatting(p, ol_match.group(2), base_size=10.5)
            i += 1
            continue
        
        # 无序列表
        ul_match = re.match(r'^[-*+]\s+(.*)', stripped)
        if ul_match:
            p = doc.add_paragraph(style='List Bullet')
            add_inline_formatting(p, ul_match.group(1), base_size=10.5)
            i += 1
            continue
        
        # 缩进的子列表项
        indent_match = re.match(r'^(\s+)[-*+]\s+(.*)', line)
        if indent_match:
            indent_level = len(indent_match.group(1)) // 2
            p = doc.add_paragraph(style='List Bullet 2' if indent_level >= 1 else 'List Bullet')
            add_inline_formatting(p, indent_match.group(2), base_size=10.5)
            i += 1
            continue
        
        # 普通段落
        p = doc.add_paragraph()
        p.paragraph_format.space_after = Pt(6)
        add_inline_formatting(p, stripped, base_size=10.5)
        i += 1
    
    # 处理末尾未关闭的代码块
    if in_code_block and code_lines:
        add_code_block(doc, code_lines, code_lang)
    
    doc.save(docx_path)
    print(f'  生成: {os.path.basename(docx_path)}')


def main():
    print('=' * 50)
    print('Markdown 转 Word 文档转换工具')
    print('=' * 50)
    
    if not os.path.exists(DOCS_DIR):
        print(f'错误: docs 目录不存在: {DOCS_DIR}')
        return
    
    # 创建输出目录
    output_dir = os.path.join(DOCS_DIR, 'word')
    os.makedirs(output_dir, exist_ok=True)
    
    success = 0
    failed = 0
    
    for md_name, docx_name in FILE_MAP.items():
        md_path = os.path.join(DOCS_DIR, md_name)
        docx_path = os.path.join(output_dir, docx_name)
        
        if not os.path.exists(md_path):
            print(f'  跳过(文件不存在): {md_name}')
            failed += 1
            continue
        
        try:
            print(f'转换: {md_name} -> {docx_name}')
            convert_md_to_docx(md_path, docx_path)
            success += 1
        except Exception as e:
            print(f'  失败: {md_name} - {e}')
            failed += 1
    
    print('=' * 50)
    print(f'转换完成: 成功 {success} 个, 失败 {failed} 个')
    print(f'输出目录: {output_dir}')
    print('=' * 50)


if __name__ == '__main__':
    main()
