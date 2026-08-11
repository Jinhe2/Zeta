ALTER TABLE terminal_operation_terminals
  ADD COLUMN IF NOT EXISTS expected_output_code VARCHAR(8) NULL COMMENT '预期试验仪语义输出代码';

ALTER TABLE terminal_operation_terminals
  MODIFY COLUMN terminal_meaning VARCHAR(128) NULL;

UPDATE terminal_operation_terminals
SET expected_output_code = CASE LOWER(TRIM(terminal_meaning))
  WHEN 'ua' THEN 'Ua' WHEN 'ub' THEN 'Ub' WHEN 'uc' THEN 'Uc' WHEN 'un' THEN 'Un'
  WHEN 'ux' THEN 'Ux' WHEN 'uy' THEN 'Uy' WHEN 'uz' THEN 'Uz' WHEN 'un2' THEN 'Un2'
  WHEN 'ia' THEN 'Ia' WHEN 'ib' THEN 'Ib' WHEN 'ic' THEN 'Ic' WHEN 'in' THEN 'In'
  WHEN 'ix' THEN 'Ix' WHEN 'iy' THEN 'Iy' WHEN 'iz' THEN 'Iz' WHEN 'in2' THEN 'In2'
  ELSE NULL
END
WHERE expected_output_code IS NULL;
