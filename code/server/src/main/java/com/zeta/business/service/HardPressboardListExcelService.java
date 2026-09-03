package com.zeta.business.service;

import com.zeta.business.entities.hardpressboardlist.HardPressboardDtos.*;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import java.io.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HardPressboardListExcelService {
  private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;
  private static final String SHEET_NAME = "硬压板基准清单";
  private static final String[] HEADERS = {
      "序号", "硬压板名称", "硬压板编号", "是否比对", "基准状态"
  };
  private final HardPressboardListService listService;

  public HardPressboardListExcelService(HardPressboardListService listService) {
    this.listService = listService;
  }

  public byte[] exportWorkbook(SettingListScopeType scopeType, Long scopeId) {
    PressboardSelectionService.requireDeviceScope(scopeType);
    ListResponse list = listService.get(scopeType, scopeId);
    try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet(SHEET_NAME);
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
      for (ItemResponse item : list.getConfiguredItems()) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(rowIndex);
        row.createCell(1).setCellValue(item.getPressboardName());
        row.createCell(2).setCellValue(item.getPressboardRef());
        row.createCell(3).setCellValue(item.isCompareEnabled() ? "是" : "否");
        row.createCell(4).setCellValue(item.isBaselineValue() ? "投入" : "退出");
        rowIndex++;
      }
      int[] widths = {10, 36, 24, 14, 16};
      for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "生成硬压板基准清单 Excel 失败", ex);
    }
  }

  public ListResponse importWorkbook(
      SettingListScopeType scopeType, Long scopeId, MultipartFile file) {
    PressboardSelectionService.requireDeviceScope(scopeType);
    if (file == null || file.isEmpty()) throw badRequest("请选择需要导入的 Excel 文件");
    String filename = file.getOriginalFilename();
    if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
      throw badRequest("仅支持 .xlsx 文件");
    }
    if (file.getSize() > MAX_FILE_SIZE) throw badRequest("Excel 文件不能超过 5MB");
    List<SaveItemRequest> items = new ArrayList<>();
    DataFormatter formatter = new DataFormatter(Locale.CHINA);
    try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
      Sheet sheet = workbook.getSheet(SHEET_NAME);
      if (sheet == null) throw badRequest("缺少“" + SHEET_NAME + "”工作表");
      validateHeader(sheet.getRow(0), formatter);
      Set<String> seen = new HashSet<>();
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) continue;
        String ref = formatter.formatCellValue(row.getCell(2)).trim();
        String state = formatter.formatCellValue(row.getCell(4)).trim();
        if (ref.isEmpty() && state.isEmpty()) continue;
        int displayRow = rowIndex + 1;
        if (ref.isEmpty()) throw badRequest("第 " + displayRow + " 行硬压板编号不能为空");
        if (!seen.add(ref)) throw badRequest("第 " + displayRow + " 行硬压板编号重复：" + ref);
        SaveItemRequest item = new SaveItemRequest();
        item.setPressboardRef(ref);
        item.setCompareEnabled(parseCompare(formatter.formatCellValue(row.getCell(3)), displayRow));
        item.setBaselineValue(parseState(state, displayRow));
        item.setSortOrder(items.size());
        items.add(item);
      }
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw badRequest("Excel 文件解析失败：" + ex.getMessage());
    }
    return listService.replace(scopeType, scopeId, items);
  }

  private void validateHeader(Row row, DataFormatter formatter) {
    if (row == null) throw badRequest("Excel 表头不能为空");
    for (int i = 0; i < HEADERS.length; i++) {
      if (!HEADERS[i].equals(formatter.formatCellValue(row.getCell(i)).trim())) {
        throw badRequest("Excel 表头格式错误，第 " + (i + 1) + " 列应为“" + HEADERS[i] + "”");
      }
    }
  }

  private boolean parseCompare(String raw, int row) {
    String value = raw == null ? "" : raw.trim();
    if ("是".equals(value) || "1".equals(value) || "true".equalsIgnoreCase(value)) return true;
    if ("否".equals(value) || "0".equals(value) || "false".equalsIgnoreCase(value)) return false;
    throw badRequest("第 " + row + " 行“是否比对”只能填写“是”或“否”");
  }

  private boolean parseState(String raw, int row) {
    String value = raw == null ? "" : raw.trim();
    if ("投入".equals(value) || "1".equals(value) || "true".equalsIgnoreCase(value)) return true;
    if ("退出".equals(value) || "0".equals(value) || "false".equalsIgnoreCase(value)) return false;
    throw badRequest("第 " + row + " 行“基准状态”只能填写“投入”或“退出”");
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
