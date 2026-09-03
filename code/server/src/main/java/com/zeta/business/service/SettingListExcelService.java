package com.zeta.business.service;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.settinglist.dto.*;
import java.io.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SettingListExcelService {
  private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;
  private static final String[] HEADERS = {"序号", "定值名称", "定值引用", "类型", "是否比对", "定值"};
  private final SettingListService settingListService;

  public SettingListExcelService(SettingListService settingListService) {
    this.settingListService = settingListService;
  }

  public byte[] exportWorkbook(SettingListScopeType scopeType, Long scopeId) {
    SettingListService.requireDeviceScope(scopeType);
    SettingListResponse list = settingListService.get(scopeType, scopeId);
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("定值清单");
      CellStyle headerStyle = workbook.createCellStyle();
      Font font = workbook.createFont();
      font.setBold(true);
      headerStyle.setFont(font);
      Row header = sheet.createRow(0);
      for (int i = 0; i < HEADERS.length; i++) {
        Cell cell = header.createCell(i);
        cell.setCellValue(HEADERS[i]);
        cell.setCellStyle(headerStyle);
      }
      int rowIndex = 1;
      for (SettingListItemResponse item : list.getConfiguredItems()) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(rowIndex);
        row.createCell(1).setCellValue(item.getSettingName());
        row.createCell(2).setCellValue(item.getSettingRef());
        row.createCell(3).setCellValue(displayValueType(item.getValueType()));
        row.createCell(4).setCellValue(item.isCompareEnabled() ? "是" : "否");
        row.createCell(5).setCellValue(item.getBaselineValue());
        rowIndex++;
      }
      int[] widths = {10, 32, 72, 14, 14, 20};
      for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "生成定值清单 Excel 失败", ex);
    }
  }

  public SettingListResponse importWorkbook(
      SettingListScopeType scopeType, Long scopeId, MultipartFile file) {
    SettingListService.requireDeviceScope(scopeType);
    if (file == null || file.isEmpty()) throw badRequest("请选择需要导入的 Excel 文件");
    String filename = file.getOriginalFilename();
    if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
      throw badRequest("仅支持 .xlsx 文件");
    }
    if (file.getSize() > MAX_FILE_SIZE) throw badRequest("Excel 文件不能超过 5MB");
    List<SettingListSaveItemRequest> items = new ArrayList<>();
    DataFormatter formatter = new DataFormatter(Locale.CHINA);
    try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
      Sheet sheet = workbook.getSheet("定值清单");
      if (sheet == null) throw badRequest("缺少“定值清单”工作表");
      boolean hasCompareColumn = validateHeader(sheet.getRow(0), formatter);
      Set<String> seen = new HashSet<>();
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) continue;
        String ref = formatter.formatCellValue(row.getCell(2)).trim();
        String value = formatter.formatCellValue(row.getCell(5)).trim();
        if (ref.isEmpty() && value.isEmpty()) continue;
        int displayRow = rowIndex + 1;
        if (ref.isEmpty()) throw badRequest("第 " + displayRow + " 行定值引用不能为空");
        if (!seen.add(ref)) throw badRequest("第 " + displayRow + " 行定值引用重复：" + ref);
        if (value.isEmpty()) throw badRequest("第 " + displayRow + " 行定值不能为空");
        SettingListSaveItemRequest item = new SettingListSaveItemRequest();
        item.setSettingRef(ref);
        item.setBaselineValue(value);
        item.setCompareEnabled(
            hasCompareColumn ? parseCompareEnabled(formatter.formatCellValue(row.getCell(4)), displayRow) : true);
        item.setSortOrder(items.size());
        items.add(item);
      }
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw badRequest("Excel 文件解析失败：" + ex.getMessage());
    }
    return settingListService.replace(scopeType, scopeId, items);
  }

  private boolean validateHeader(Row row, DataFormatter formatter) {
    if (row == null) throw badRequest("Excel 表头不能为空");
    for (int i = 0; i < 4; i++) {
      if (!HEADERS[i].equals(formatter.formatCellValue(row.getCell(i)).trim())) {
        throw badRequest("Excel 表头格式错误，第 " + (i + 1) + " 列应为“" + HEADERS[i] + "”");
      }
    }
    String fifth = formatter.formatCellValue(row.getCell(4)).trim();
    if (!"是否比对".equals(fifth) && !"功能约束".equals(fifth)) {
      throw badRequest("Excel 表头格式错误，第 5 列应为“是否比对”");
    }
    if (!HEADERS[5].equals(formatter.formatCellValue(row.getCell(5)).trim())) {
      throw badRequest("Excel 表头格式错误，第 6 列应为“定值”");
    }
    return "是否比对".equals(fifth);
  }

  private boolean parseCompareEnabled(String raw, int row) {
    String value = raw == null ? "" : raw.trim();
    if ("是".equals(value) || "1".equals(value) || "true".equalsIgnoreCase(value)) return true;
    if ("否".equals(value) || "0".equals(value) || "false".equalsIgnoreCase(value)) return false;
    throw badRequest("第 " + row + " 行“是否比对”只能填写“是”或“否”");
  }

  private String displayValueType(String valueType) {
    if ("FLOAT".equalsIgnoreCase(valueType)) return "浮点型";
    if ("INTEGER".equalsIgnoreCase(valueType)) return "整数型";
    return valueType;
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
