-- Tmms 在模型目录中以毫秒整型承载，定值基准换算为秒后统一按浮点型处理。
UPDATE setting_list_item
SET value_type = 'FLOAT'
WHERE setting_ref LIKE BINARY '%Tmms%'
  AND value_type <> 'FLOAT';
