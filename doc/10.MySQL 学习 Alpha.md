  MySQL 学习笔记Alpha



>  有个需要特别注意的地方，这里重点提出：**本文档部分的内容可能会过时！**
>
> 这个文档绝大部分是参考《高性能 MySQL》写的，可以说就是一个读书笔记。书中反复强调，绝大部分内容是针对 MySQL 5.5 书写的。随着 MySQL 的发展，本文档部分的内容可能会过时。所以，文档如果有错误之处，还请以相应版本的官方文档为准！



## 1. Schema 设计

良好的逻辑设计和物理设计是高性能的基石。

### 1.1. 数据类型的选择

1. 更小的通常更好
2. 简单就好
3. 尽量避免 Null

InnoDB 使用单独的位存储 Null 值，所以对于稀疏数据（多数为 Null，少数非 Null）有很好的空间效率。

MySQL 很多数据类型只是别名，可以用 `SHOW CREATE TABLE` 查看对应的基本类型。

#### 1.1.1. 整数

整数类型： `TINYINT` 、 `SMALLINT` 、 `MEDIUMINT` 、 `INT` 、 `BIGINT`；分别使用 8、16、24、32、64 位存储空间。存储的范围从 -2(N-1) 到 2(N-1)-1。

整数类型有可选的 `UNSIGNED`，表示不允许负值。

有符号和无符号类型使用相同的存储空间，并具有相同的性能，因此可以根据实际情况选择合适的类型。

MySQL 可以为整数类型指定宽度，例如 `INT(11)`，这实际没有意义：它不会限制值的合法范围。对于存储和计算来说， `INT(1)` 和 `INT(20)` 是相同的。

#### 1.1.2. 实数

`DECIMAL` 类型用于存储精确的小数。CPU 不支持对 `DECIMAL` 的直接计算。

CPU 直接支持原生浮点计算，所以浮点运算明显更快。

MySQL 5.0 和更高版本中的 `DECIMAL` 类型运行最多 65 个数字。

代码 1. 测试 `DECIMAL` 类型的最大数字数

```mysql
DROP TABLE IF EXISTS decimal_test;
CREATE TABLE decimal_test (
  col1 DECIMAL(65, 10),
  col2 DECIMAL(66, 10) 
);
```

|      | 执行时报错，改为65即可执行。 |
| ---- | ---------------------------- |

浮点类型在存储同样范围的值时，通常比 `DECIMAL` 使用更少的空间。 `FLOAT` 使用 4 个字节存储； `DOUBLE` 占用 8 个字节。

MySQL 使用 `DOUBLE` 作为内部浮点计算的类型。

因为需要额外的空间和计算开销，所以应该尽量只在对小数进行精确计算时才使用 `DECIMAL` 。

在数据量比较大的时候，可以考虑使用 `BIGINT` 代替 `DECIMAL` ，将需要存储的货币单位根据小数的位数乘以相应的倍数即可。

#### 1.1.3. 字符串类型

从 MySQL 4.1 开始，每个字符串列可以定义自己的字符集和排序规则，或者说校对规则。

- `VARCHAR`

  用于存储可变长字符串，比定长类型更节省空间。

  > 例外：MySQL表使用 `ROW_FORMAT=FIXED` 创建的话，每一行都会使用定长存储。

  `VARCHAR` 需要使用 1 或 2个额外字节记录字符串的长度：如果列的最大长度小于或者等于255字节，则只使用1个字节表示，否则使用 2 个字节。

  `VARCHAR` 节省了存储空间，所以对性能也有帮助。但是，行变长时，如果页内没有更多的空间可以存储，MyISAM 会将行拆成不同的片段存储，InnoDB 则需要分裂页来使行可以放进页内。

  > 每页最多能存多少数据？ 28 = 256， 216 = 65536。数据能否超过65536？如果不能，超过了会怎么样？-- MySQL 中 `VARCHAR` 类型的最大长度限制为 65535。

  上面只是计算出来的结果，我们使用建表语句测试 `VARCHAR` 类型的最大长度限制。

  ```mysql
  CREATE TABLE varchar_test
  (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    varchar_field VARCHAR(65535)  DEFAULT ''
  );
  ```

  

  执行，结果报错：

  ```bash
  Column length too big for column 'varchar_field' (max == 21845); use BLOB or TEXT instead
  ```

  

  但是，如果把字段长度改为 `21845`，然后结果就成这样了：

  ```bash
  Row size too large. The maximum row size for the used table type, not counting BLOBs, is 65535. This includes storage overhead, check the manual. You have to change some columns to TEXT or BLOBs
  ```

  

  **`VARCHAR` 类型的最大长度限制到底是多少呢？**

  InnoDB 更灵活，可以把过长的 `VARCHAR` 存储为 `BLOB`。

  > 变化的阈值是多少？

- `CHAR`

  定长，根据定义分配足够的空间。当存储 `CHAR` 值时，MySQL 会删除所有的末尾空格。`CHAR` 值会根据需要采用空格进行填充以方便比较。`CHAR` 适合存储很短的字符串，或者所有值都接近同一个长度，比如密码的 MD5 值。对于经常变更的数据， `CHAR` 也比 `VARCHAR` 更好，定长不容易产生碎片。非常短的列， `CHAR` 比 `VARCHAR` 在存储空间上更有效率。

代码 2. 测试数据两端的空格保留情况

```mysql
# 测试 CHAR
DROP TABLE IF EXISTS char_test;
CREATE TABLE char_test (char_col CHAR(10));

INSERT INTO char_test VALUES ('string1'), ('   string2'), ('string3   ');
# 注意观察查询结果中字符串两边的空格变化。
SELECT CONCAT("'", char_col, "'") FROM char_test; 

# 测试 VARCHAR
DROP TABLE IF EXISTS varchar_test;
CREATE TABLE varchar_test (varchar_col VARCHAR(10));

INSERT INTO varchar_test VALUES ('string1'), ('   string2'), ('string3   ');
# 注意观察查询结果中字符串两边的空格变化。
SELECT CONCAT("'", varchar_col, "'") FROM varchar_test; 

```

|      |      |
| ---- | ---- |

**数据如何存储取决于存储引擎。**

与 `CHAR` 和 `VARCHAR` 类似的类型还有 `BINARY` 和 `VARBINARY`，它们存储的是二进制字符串。二进制字符串存储的是字节码而不是字符。MySQL 填充 `BINARY` 采用的是 `\0` （零字节）而不是空格，在检索时也不会去掉填充值。

二进制比较的优势并不仅仅体现在大小写敏感上。MySQL 比较 `BINARY` 字符串时，每次按一个字节，并且根据该字节的数值进行比较。因此，二进制比字符串比较简单很多，所以也更快。

|      | 慷慨是不明智的。 |
| ---- | ---------------- |

##### 1.1.3.1. BLOB和TEXT 类型

`BLOB` 和 `TEXT` 都是为存储很大的数据而设计的字符串数据类型，分别采用二进制和字符串方式存储。

字符串类型： `TINYTEXT`、 `SMALLTEXT`、 `TEXT`、 `MEDIUMTEXT`、 `LONGTEXT`
二进制类型： `TINYBLOB`、 `SMALLBLOB`、 `BLOB`、 `MEDIUMBLOB`、 `LONGBLOB`

`BLOB` 是 `SMALLBLOB` 的同义词； `TEXT` 是 `SMALLTEXT` 的同义词。

MySQL 把每个 `BLOB` 和 `TEXT` 值当做一个独立的对象处理。InnoDB 会使用专门的“外部”存储区域来进行存储，此时每个值在行内需要 1 ~ 4 个字节存储一个指针，然后在外部存储区域存储实际的值。

`BLOB` 和 `TEXT` 家族之间仅有的不同是 `BLOB` 类型存储的是二进制，没有排序规则或字符集，而 `TEXT` 类型有字符集和排序规则。

`BLOB` 和 `TEXT` 只对每个列的最前 `max_sort_length` 字节而不是整个字符串做排序。

MySQL 不能将 `BLOB` 和 `TEXT` 列全部长度的字符串进行索引。

##### 1.1.3.2. 使用枚举（ENUM）代替字符串

枚举列可以把一些不重复的字符串存储成一个预定义的集合。MySQL 在存储枚举时非常紧凑，会根据列表值的数量压缩到一个或者两个字节中。MySQL 在内部会将每个值在列表中的位置保存为整数，并且在表的 *.frm* 文件中保存 “数字-字符串” 映射关系的 “查找表”。

代码 3. 测试枚举的存储值

```mysql
DROP TABLE IF EXISTS enum_test;
CREATE TABLE enum_test (e ENUM ('fish', 'apple', 'dog'));
# 三行数据实际存储为整数，而不是字符串。
INSERT INTO enum_test (e) VALUES ('fish'), ('dog'), ('apple'); 

SELECT e + 0 FROM enum_test;
# 测试排序性
SELECT e FROM enum_test ORDER BY e; 
# 根据定义的字符串排序
SELECT e FROM enum_test ORDER BY field(e, 'apple', 'dog', 'fish'); 

```

如果使用数字作为 `ENUM` 枚举常量，很容易导致混乱。尽量避免这么做。

枚举字段是按照内部存储的整数而不是定义的字符串进行排序的。一种绕过这种限制的方式是按照需要的顺序来定义枚举列。也可以在查询中使用 `FIELD()` 函数显式地指定排序顺序，但是会导致 MySQL 无法利用索引消除排序。

枚举最不好的地方是，字符串列表是固定的，添加或删除字符串必须使用 `ALTER TABLE`。在 MySQL 5.1 中支持只在列表末尾添加元素，而不用重建整个表。

把枚举保存为整数，必须查找才能转换为字符串，有开销。尤其和字符串的列关联查询时，甚至不如字符串关联字符性能好。

通用的设计实践：在“查找表”时采用整数主键而避免采用基于字符串进行关联。

根据 `SHOW TABLE STATUS` 命令输出结果中 `Data_length` 列的值，把列转换为 `ENUM` 可以让表的大小缩小.

#### 1.1.4. 日期和时间类型

MySQL 能存储的最小时间粒度为秒。但，也可以使用微秒级的粒度进行临时运算。

- `DATETIME`

  保存大范围的值，从 1001 年到 9999 年，精度为秒。把日期和时间封装到格式为 YYYYMMDDHHMMSS 的整数中，与时区无关。使用 8 个字节的存储空间。

- `TIMESTAMP`

  保存从 1970 年 1 月 1 日午夜以来的秒数，和 UNIX 时间戳相同。`TIMESTAMP` 只使用 4 个字节的存储空间，范围是从 1970 年到 2038 年。

MySQL 4.1 以及更新的版本按照 `DATETIME` 的方式格式化 `TIMESTAMP` 的值。`TIMESTAMP` 的存储格式在各个版本都是一样的。

`TIMESTAMP` 显示的值也依赖于时区。MySQL 服务器、操作系统以及客户端连接都有时区设置。因此，存储值为 0 的 `TIMESTAMP` 在美国东部时区显示为 “1969-12-31 19:00:00”，与格林尼治时间差5个小时。

如果在多个时区存储或访问数据， `TIMESTAMP` 和 `DATETIME` 的行为将会很不一样。前者提供的值与时区有关，后者则保留文本表示的日期和时间。

|      | 如果在东八区保存为 2016年12月05日17:34:17，在格林尼治显示为多少？ |
| ---- | ------------------------------------------------------------ |

默认情况下，如果插入时没有指定第一个 `TIMESTAMP` 列的值，MySQL 则设置这个列的值为当前时间。

`TIMESTAMP` 列默认为 `NOT NULL`。

通常应该尽量使用 `TIMESTAMP` ，因为它比 `DATETIME` 空间效率更高。

可以使用 `BIGINT` 类型存储微秒级别的时间戳，或者使用 `DOUBLE` 存储秒之后的小数部分。

#### 1.1.5. 位数据类型

#### 1.1.6. 选择标识符（键列）

更有可能使用标识列与其他值进行比较，或者通过标识列寻找其他列。

选择标识列的类型时，不仅仅需要**考虑存储类型**，还需要**考虑 MySQL 对这种类型怎么执行计算和比较**。

一旦选定一种类型，要确保在所有关联表中都使用同样的类型。类型之间需要精确匹配，包括像 `UNSIGNED` 这样的属性。混用不同数据类型可能导致性能问题，在比较操作时隐式类型转换也可能导致很难发现的错误。

在可以满足值的范围的需求，并且预留为了增长空间的前提下，应该选择最小的数据类型。

- 整数类型

  整数通常是标识列最好的选择，因为它们很快并且可以使用 `AUTO_INCREMENT`。

- `ENUM` 和 `SET` 类型

  通常是一个糟糕的选择。 `ENUM` 和 `SET` 列适合存储固定信息。

- 字符串类型

  如果可能，应该避免使用字符串作为标识列，因为它们很消耗空间，并且通常比数字类型慢。MyISAM 默认对字符串使用压缩索引，这会导致查询慢很多。使用完全“随机”的字符串也需要多加注意，例如 MD5()、SHA1()、 UUID()产生的字符串。这些新值会任意分布在很大的空间内，这会导致 `INSERT` 以及一些 `SELECT` 语句变得很慢：插入值会随机地写到索引的不同位置，所以使得 `INSERT` 语句更慢。这会导致页分裂、磁盘随机访问，以及对于聚簇存储引擎产生聚簇索引碎片。`SELECT` 语句会变得更慢，因为逻辑上相邻的行会分布在磁盘和内存的不同地方。随机值导致缓存对所有类型的查询语句效果都很差，因为会使得缓存赖以工作的局部访问性原理失效。如果真个数据集都一样的“热”，那么缓存任何一部分特别数据到内存都没有好处；如果工作集比内存大，缓存将会有很多刷新和不命中。

如果存储 UUID 值，则应该移除 “-” 符号；更好的做法是，使用 `UNHEX()` 函数转换 UUID 值为 16 字节的数字，并且存储在一个 `BINARY(16)` 列中。检索时可以通过 `HEX()`函数来格式化为十六进制格式。

UUID 值还是有一定的顺序的。

#### 1.1.7. 特殊类型数据

- 低于秒级精度的时间戳
- IPv4 地址 — `INET_ATON()` 和 `INET_NTOA()`。

### 1.2. MySQL Schema 设计中的陷阱

- 太多的列

  MySQL 的存储引擎 API 工作时需要在服务器层和存储引擎层之间通过行缓冲格式拷贝数据，然后在服务器层将缓冲内容解码成各个列。从行缓冲中将解码过的列转换成行数据结构的操作代价是非常高的。 MyISAM 定长行结构正好匹配，不需要转换。MyISAM 的变长行结构和 InnoDB 的行结构则总是需要转换。**转换的代价依赖于列的数量。**

- 太多的关联

  MySQL 限制了每个关联操作最多只能有 61 张表。一个粗略的经验法则，如果希望查询执行得快速且并发性好，单个查询最好在 12 个表以内做关联。

- 全能的枚举

  注意防止过度使用枚举。修改枚举，就需要 `ALTER TABLE`，在 5.1 和更新版本中，只有在末尾增加值时，不需要 `ALTER TABLE`。

- 变相的枚举

  枚举列允许在列中存储一组定义值中的单个值，集合（ `SET` ）列则允许在列中存储一组定义值中的一个或多个值。比如: `CREATE TABLE set_test ( is_default SET ('Y', 'N') NOT NULL DEFAULT 'N' );` 真假只有一个，定义为枚举更好。

- 非此发明的 NULL

  建议不要存 NULL。但是不要走极端。当确实需要表示未知值时也不要害怕使用 NULL。处理 NULL 确实不容易，但有时候会比它的替代方案更好。

### 1.3. 范式和反范式

- 第一范式

  符合1NF的关系中的每个属性都不可再分。1NF是所有关系型数据库的最基本要求。

[解释一下关系数据库的第一第二第三范式？ - 刘慰的回答 - 知乎](https://www.zhihu.com/question/24696366/answer/29189700)

**范式化通常带来的好处：**

- 范式化的更新操作通常比反范式化要快。
- 当数据较好地范式化时，就只有很少或者没有重复数据，所以只需要修改更少的数据。
- 范式化的表通常更小，可以更好地存放在内存里，所以执行操作会更快。
- 很少有多余的数据意味着检索列表数据时，更少需要 `DISTINCT` 或者 `GROUP BY` 语句。

范式化设计的 Schema 的缺点是通常需要关联。

**反范式的优缺点**

- 反范式化的 Schema 因为所有数据都在一张表中，可以很好地避免关联。
- 单独的表也能使用更有效的索引策略。

**混用范式化和反范式化**

完全的范式化和完全的反范式化 Schema 都是实验室里才有的东西。在实际应用中经常需要混用，可能使用部分范式化的 Schema、缓存表，以及其他技巧。

最常见的反范式化数据的方法是复制或者缓存，在不同的表中存储相同的特定列。

从父表冗余一些数据到子表的利益是排序的需要。

缓存衍生值也是有用的。

### 1.4. 缓存表和汇总表

有时提升性能最好的方法是在同一张表中保存衍生的冗余数据；有时也需要创建一张完全独立的汇总表或缓存表。

缓存表表示存储那些可以比较简单地从 Schema 其他表获取数据的表。
汇总表表示保存的是使用 `GROUP BY` 语句聚合数据的表。

一个有用的技巧是对缓存表使用不同的存储引擎。例如：主表用 InnoDB，使用 MyISAM 作为缓存表的引擎将会得到更小的索引占用空间，并且可以做全文检索。

|      | 全文检索还是使用专门的工具，比如 ElasticSearch 更好。 |
| ---- | ----------------------------------------------------- |

在使用缓存表和汇总表时，必须决定是实时维护数据还是定时重建。看需求。定时重建不仅节省资源，还保持表不会有很多碎片，以及完全顺序组织的索引（这会更加高效）。

当重建汇总表和缓存表时，使用“影子表”来保证数据在操作时依然可用。

```mysql
DROP TABLE IF EXISTS my_summary_new, my_summary_old;

CREATE TABLE my_summary_new LIKE my_summary;

-- TODO：执行汇总操作

RENAME TABLE my_summary TO my_summary_old, my_summary_new TO my_summary;

```

#### 1.4.1. 物化视图

物化视图是预先计算并且存储在磁盘上的表，可以通过各种各样的策略刷新和更新。

MySQL 并不原生支持物化视图。

Justin Swanhart 的开源工具 Flexviews， [Swanhart Toolkit](https://github.com/greenlion/swanhart-tools)。

#### 1.4.2. 计数器表

可以利用 `CurrentHashMap` 分段锁的思想，将对同一个计算器的修改，打散到多个变量上，然后在求和。

```mysql
DROP TABLE IF EXISTS hit_counter;
CREATE TABLE hit_counter (
  slot TINYINT UNSIGNED NOT NULL  PRIMARY KEY,
  cnt  INT UNSIGNED     NOT NULL
)ENGINE = InnoDB;

UPDATE hit_counter SET cnt = cnt + 1 WHERE slot = RAND() * 100;

SELECT SUM(cnt) FROM hit_counter;
```

一个常见需要时每个一段时间开始一个新的计算器（例如，每天一个）。

```mysql
DROP TABLE IF EXISTS daily_hit_counter;
CREATE TABLE daily_hit_counter (
  day  DATE             NOT NULL,
  slot TINYINT UNSIGNED NOT NULL,
  cnt  INT UNSIGNED     NOT NULL,
  PRIMARY KEY (day, slot)
)ENGINE = InnoDB;

-- 插入数据
INSERT INTO daily_hit_counter (day, slot, cnt)
VALUES (current_date, rand() * 100, 1)
ON DUPLICATE KEY UPDATE cnt = cnt + 1;

-- 定期执行：合并所有结果到 0 号槽，并且删除所有其他的槽：
UPDATE daily_hit_counter AS c
  INNER JOIN (
               SELECT
                 day,
                 sum(cnt)  AS cnt,
                 min(slot) AS mslot
               FROM daily_hit_counter
               GROUP BY day
             ) AS x USING (day)
SET c.cnt = if(c.slot = x.mslot, x.cnt, 0),
  c.slot  = if(c.slot = x.mslot, 0, c.slot);
DELETE FROM daily_hit_counter WHERE slot <> 0 AND cnt = 0;

```

|      | 为了提升度查询的速度，可以建立额外索引；这样会增加些查询的负担，虽然写的慢，但是更显著提高了读操作的性能。 |
| ---- | ------------------------------------------------------------ |

### 1.5. 加快 `ALTER TABLE` 操作的速度

MySQL 的 `ALTER TABLE` 操作的性能对于大表来说是个大问题。 MySQL 执行大部分修改表结构操作的方法是用新的结构创建一个空表，从旧表中查出所有数据插入新表，然后删除旧表。

一般而言，大部分 `ALTER TABLE` 操作将导致 MySQL 服务中断。有两个技巧可以避免：

- 先在一台不提供服务的机器上执行 `ALTER TABLE` 操作，然后和提供服务的主库进行切换；
- 影子拷贝：用要求的表结构创建一张和源表无关的新表，然后通过重命名和删表的操作交换两张表。还有一些第三方工具可以完成：
  - Facebook [online schema change](https://launchpad.net/mysqlatfacebook)
  - Shlomi Noach [openark toolkit](http://code.openark.org/)
  - [Percona Toolkit](http://www.percona.com/software)

不是所有的 `ALTER TABLE` 操作都会引起表重建。

```mysql
-- 很慢，N 多次读和 N 多次插入操作
ALTER TABLE film
  MODIFY COLUMN rental_duration TINYINT(3) NOT NULL DEFAULT 5;

-- 直接修改 _.frm_ 文件而不设计表数据。操作非常快。
ALTER TABLE film
  ALTER COLUMN rental_duration SET DEFAULT 5;
```

|      | `ALTER TABLE` 允许使用 `ALTER COLUMN`、 `MODIFY COLUMN` 和 `CHANGE COLUMN` 语句修改列。这三种操作都是不一样的。 *有什么不一样呢？* |
| ---- | ------------------------------------------------------------ |

#### 1.5.1. 只修改 *.frm* 文件

下面的这些操作有可能不需要重建表：

- 移除一个列的 `AUTO_INCREMENT` 属性；
- 增加、移除，或更改 `ENUM` 和 `SET` 常量。

基本的技术是为想要的表结构创建一个新的 *.frm* 文件，然后用它替换掉已经存在的那张表的 *.frm* 文件。步骤如下：

1. 创建一张有相同结构的空表，并进行所需要的修改；
2. 执行 `FLUSH TABLES WITH READ LOCK`。这将会关闭所有正在使用的表，并且禁止任何表被打开；
3. 交换 *.frm* 文件；
4. 执行 `UNLOCK TABLES` 来释放第2步的读锁。

#### 1.5.2. 快速创建 MyISAM 索引

为了高效地载入数据到 MyISAM 表中，有一个常用的技巧是先禁用索引、载入数据，然后重新启用索引。

```mysql
ALTER TABLE load_data DISABLE KEYS;

-- 载入数据

ALTER TABLE load_data ENABLE KEYS;

```

不过，这个办法对唯一索引无效，因为 `DISABLE KEYS` 只对非唯一索引有效。

现代版本的 InnoDB 中有类似的技巧。

### 1.6. 总结

- 尽量避免过度设计；
- 使用小而简单的合适数据类型，除非真的需要，否则应尽可能避免使用 `NULL`；
- 尽量使用相同的数据类型存储相似或相关的值，尤其是要在关联条件中使用的列；
- 注意可变长字符串，其在临时表和排序时可能导致悲观的按最大长度分配内存；
- 尽量使用整型定义标识列；
- 避免使用 MySQL 已经遗弃的特性，例如指定浮点数的精度，或者整型的显示宽度；
- 小心使用 `ENUM` 和 `SET`；
- 最好避免使用 `BIT`。

## 2. 事务

事务是一组原子性的 SQL 查询，或者说是一个独立的工作单元。事务内的所有操作要么全部执行成功，要么全部执行失败。

### 2.1. 四个基本特性

- **Atomicity（原子性）**：事务是一个不可分割的整体，事务内所有操作要么全部提交成功，要么全部失败回滚。
- **Consistency（一致性）**：事务执行前后，数据从一个状态到另一个状态必须是一致的（A向B转账，不能出现A扣了钱，B却没收到）。
- **Isolation（隔离性）**：多个并发事务之间相互隔离，不能互相干扰。或者说一个事务所做的修改在最终提交以前，对其他事务是不可见的。
- **Durablity（持久性）**：事务完成后，对数据库的更改是永久保存的，不能回滚。

### 2.2. 事务隔离级别

#### 2.2.1. Read Uncommitted(未提交读)

在 Read Uncommitted 级别，事务中的修改，即使没有提交，对其他事务也都是可见的。事务可以读取未提交的数据，这也被称为**脏读(Dirty Read)**。性能不会好太多，但是问题却一大堆，实际应用中一般很少使用。

#### 2.2.2. Read Committed(提交读)

大多数数据库系统的默认隔离级别都是 Read Committed。Read Committed 满足前面提到的隔离性的简单定义：一个事务开始时，只能“看见”已经提交的事务所做的修改。换句话说：一个事务从开始直到提交之前，所做的任何修改对其他事务都是不可见的。有时也叫不可重复读(Nonrepeatable Read)。

#### 2.2.3. Repeatable Read(可重复读)

Repeatable Read 解决了脏读的问题。但是还是无法解决领一个**幻读(Phantom Read)**问题。所谓幻读，指的是当某个事务在读取某个范围内的记录时，另外一个事务又在该范围内插入了新的记录，当之前的事务再次读取该范围的记录时，会产生幻行(Phantom Row)。InnoDB 和 XtraDB 存储引擎通过多版本并发控制(MVCC，Multiversion Concurrency Control)解决了幻读的问题。

#### 2.2.4. Serializable(可串行化)

Serializable 是最高的隔离级别。它通过强制事务串行执行，避免了前面说的幻读问题。简单来说，Serializable 会在读取的每一行数据上都加锁，所以导致大量的超时和锁争用的问题。实际中，极少使用。

Repeatable Read(可重复读) 是 MySQL 默认事务隔离级别。

### 2.3. 常见错误

#### 2.3.1. Phantom Read(幻读)

B 事务读取了两次数据，在这两次的读取过程中A事务添加了数据，B 事务的这两次读取出来的集合不一样。幻读产生的流程如下：

 ![image-20220123171455086](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123171455086-d99eef.png)

图 1. 幻读处理流程

这个流程看起来和不可重复读差不多，但幻读强调的集合的增减，而不是单独一条数据的修改。

#### 2.3.2. NonRepeatable Read(不可重复读)

B 事务读取了两次数据，在这两次的读取过程中 A 事务修改了数据，B 事务的这两次读取出来的数据不一样。B 事务这种读取的结果，即为不可重复读（Nonrepeatable Read）。相反，“可重复读”在同一个事务中多次读取数据时，能够保证所读数据一样，也就是后续读取不能读到另一个事务已提交的更新数据。不可重复读的产生的流程如下：

![image-20220123171531787](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123171531787-ea0be2.png)

图 2. 不可重复读处理流程

#### 2.3.3. Dirty Read(脏读)

A 事务执行过程中，B 事务读取了A事务的修改。但是由于某些原因，A 事务可能没有完成提交，发生 RollBack 了操作，则B事务所读取的数据就会是不正确的。这个未提交数据就是脏读（Dirty Read）。

![脏读处理流程](https://notes.diguage.com/mysql/assets/images/dirty-read-process.png)

图 3. 脏读处理流程

#### 2.3.4. Lost Update(第一类丢失更新)

在完全未隔离事务的情况下，两个事务更新同一条数据资源，某一事务完成，另一事务异常终止，回滚造成第一个完成的更新也同时丢失 。这个问题现代关系型数据库已经不会发生。

#### 2.3.5. Lost Update(第二类丢失更新)

不可重复读有一种特殊情况，两个事务更新同一条数据资源，后完成的事务会造成先完成的事务更新丢失。这种情况就是大名鼎鼎的第二类丢失更新。主流的数据库已经默认屏蔽了第一类丢失更新问题（即：后做的事务撤销，发生回滚造成已完成事务的更新丢失），但我们编程的时候仍需要特别注意第二类丢失更新。它产生的流程如下：

![image-20220123171552530](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123171552530-6da740.png)

图 4. Lost Update(第二类丢失更新)

#### 2.3.6. 小结

![image-20220123171607604](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123171607604-948346.png)

图 5. “读”之间的关系

![image-20220123171618968](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123171618968-ac7480-4004ac.png)

图 6. 数据库事务总结

### 2.4. `Read Committed` vs `Repeatable Read`

Read Committed(提交读，也称为不可重复读)和 Repeatable Read(可重复读)的区别在于，前者在本事务未提交之前其他事务的增删改操作提交后会影响读的结果。读的是最新结果。

Repeatable Read(可重复读)在读的过程中数据始终是事务启动时的数据状态，未提交之前其他事物的增删改操作提交后都不会影响读的结果。读的是快照结果。

```mysql
CREATE TABLE `member`
(
    `id`       BIGINT(20) NOT NULL AUTO_INCREMENT,
    `name`     VARCHAR(100) DEFAULT '',
    `birthday` DATETIME     DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

mysql> SELECT * FROM member;
+----+-----------------+---------------------+
| id | name            | birthday            |
+----+-----------------+---------------------+
|  1 | D瓜哥           | 2018-12-26 06:02:57 |
|  2 | www.diguage.com | 2019-09-26 00:00:00 |
+----+-----------------+---------------------+

```

#### 2.4.1. `READ COMMITTED` 更新操作

1. A 开始事务

   ```mysql
   mysql> SET autocommit=0;
   Query OK, 0 rows affected (0.00 sec)
   
   mysql> SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
   Query OK, 0 rows affected (0.00 sec)
   # A 开启事务
   mysql> BEGIN; 
   Query OK, 0 rows affected (0.00 sec)
   
   ```

   

2. A 第一次查询

   ```mysql
   mysql> SELECT * FROM member;
   +----+-----------------+---------------------+
   | id | name            | birthday            |
   +----+-----------------+---------------------+
   |  1 | D瓜哥           | 2018-12-26 06:02:57 |
   |  2 | www.diguage.com | 2019-09-26 00:00:00 |
   +----+-----------------+---------------------+
   2 rows in set (0.00 sec)
   
   ```

   

3. B 开始事务，并查询修改，然后提交事务

   ```mysql
   mysql> SET autocommit = 0;
   Query OK, 0 rows affected (0.00 sec)
   
   mysql> SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
   Query OK, 0 rows affected (0.01 sec)
   #	B 开启事务
   mysql> BEGIN; 
   Query OK, 0 rows affected (0.00 sec)
   # B 在事务中，更新数据
   mysql> UPDATE member 
       -> SET name = 'https://www.diguage.com/'
       -> WHERE id = 2;
   Query OK, 1 row affected (0.00 sec)
   Rows matched: 1  Changed: 1  Warnings: 0
   # B 提交事务
   mysql> COMMIT; 
   Query OK, 0 rows affected (0.01 sec)
   ```

   

4. A 再次查询，出现读不一致

   ```mysql
   mysql> SELECT *
       -> FROM member
       -> WHERE id = 2;
   +----+--------------------------+---------------------+
   | id | name                     | birthday            |
   +----+--------------------------+---------------------+
   |  2 | https://www.diguage.com/ | 2019-09-26 00:00:00 |
   +----+--------------------------+---------------------+
   1 row in set (0.00 sec)
   ```

   

#### 2.4.2. `READ COMMITTED` 新增操作

1. A 开始事务

   ```mysql
   mysql> SET autocommit=0;
   Query OK, 0 rows affected (0.00 sec)
   
   mysql> SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
   Query OK, 0 rows affected (0.00 sec)
   # 	A 开启事务
   mysql> BEGIN; 
   Query OK, 0 rows affected (0.00 sec)
   ```

   

2. A 第一次查询

   ```mysql
   mysql> SELECT *
       -> FROM member;
   +----+--------------------------+---------------------+
   | id | name                     | birthday            |
   +----+--------------------------+---------------------+
   |  1 | D瓜哥                    | 2018-12-26 06:02:57 |
   |  2 | https://www.diguage.com/ | 2019-09-26 00:00:00 |
   +----+--------------------------+---------------------+
   2 rows in set (0.00 sec)
   
   ```

   

3. B 开始事务，并查询修改，然后提交事务

   ```mysql
   mysql> SET autocommit = 0;
   Query OK, 0 rows affected (0.00 sec)
   
   mysql> SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
   Query OK, 0 rows affected (0.01 sec)
   # B 开启事务
   mysql> BEGIN; 
   Query OK, 0 rows affected (0.00 sec)
   # B 在事务中，新增数据
   mysql> INSERT INTO member(name, birthday) 
       ->     VALUE ('diguage', '2020-03-25 14:43:34');
   Query OK, 1 row affected (0.01 sec)
   # B 提交事务
   mysql> COMMIT; 
   Query OK, 0 rows affected (0.01 sec)
   ```

   

4. A 再次查询，出现读不一致

   ```mysql
   mysql> SELECT * FROM member;
   +----+--------------------------+---------------------+
   | id | name                     | birthday            |
   +----+--------------------------+---------------------+
   |  1 | D瓜哥                    | 2018-12-26 06:02:57 |
   |  2 | https://www.diguage.com/ | 2019-09-26 00:00:00 |
   |  3 | diguage                  | 2020-03-25 14:43:34 |
   +----+--------------------------+---------------------+
   3 rows in set (0.00 sec)
   ```

   

#### 2.4.3. `REPEATABLE READ` 更新操作

1. A 开始事务

   ```mysql
   mysql> SET autocommit=0;
   Query OK, 0 rows affected (0.00 sec)
   
   mysql> SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
   Query OK, 0 rows affected (0.00 sec)
   # A 开启事务
   mysql> BEGIN; 
   Query OK, 0 rows affected (0.00 sec)
   ```

   

2. A 第一次查询

   ```mysql
   mysql> SELECT *
       -> FROM member;
   +----+--------------------------+---------------------+
   | id | name                     | birthday            |
   +----+--------------------------+---------------------+
   |  1 | D瓜哥                    | 2018-12-26 06:02:57 |
   |  2 | https://www.diguage.com/ | 2019-09-26 00:00:00 |
   |  3 | diguage                  | 2020-03-25 14:43:34 |
   +----+--------------------------+---------------------+
   3 rows in set (0.00 sec)
   
   ```

   

3. B 开始事务，并查询修改，然后提交事务

   ```mysql
   mysql> SET autocommit = 0;
   Query OK, 0 rows affected (0.00 sec)
   
   mysql> SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
   Query OK, 0 rows affected (0.01 sec)
   # B 开启事务
   mysql> BEGIN; 
   Query OK, 0 rows affected (0.00 sec)
   # B 在事务中，更新数据
   mysql> UPDATE member 
       -> SET name = 'https://github.com/diguage/'
       -> WHERE id = 2;
   Query OK, 1 row affected (0.00 sec)
   Rows matched: 1  Changed: 1  Warnings: 0
   # B 提交事务
   mysql> COMMIT; 
   Query OK, 0 rows affected (0.01 sec)
   ```

   

4. B 更新后，A 再次查询，读取的仍是 B 没有改变的数据

   ```mysql
   mysql> SELECT * FROM member;
   +----+--------------------------+---------------------+
   | id | name                     | birthday            |
   +----+--------------------------+---------------------+
   |  1 | D瓜哥                    | 2018-12-26 06:02:57 |
   |  2 | https://www.diguage.com/ | 2019-09-26 00:00:00 |
   |  3 | diguage                  | 2020-03-25 14:43:34 |
   +----+--------------------------+---------------------+
   3 rows in set (0.00 sec)
   ```

   

### 2.5. 实现原理

InnoDB 使用 MVCC 来解决幻读问题。MVCC 的实现，是通过保存数据在某个时间点的快照来实现的。不管需要执行多长时间，每个事务看到的数据都是一致的。根据事务开始的时间不同，每个事务对同一张表，同一时刻看到的数据可能都是不一样的。 MVCC 只能在 Repeatable Read 和 Read Committed 下工作，其他级别和 MVCC 不兼容。

InnoDB 的 MVCC，是通过在每行记录后面保存两个隐藏的列来实现的。一个保存了行的创建时间，一个保存行的过期时间（或删除时间）。实际保存的是系统版本号（system version number）。每开始一个新的事务，系统版本号就会自动递增。事务开始时刻的系统版本号会作为事务的版本号，用来和查询到的每行记录的版本号进行比较。

- SELECT

  InnoDB 会根据以下两个条件检查每行记录：InnoDB 只查找版本早于当前事务版本的数据行（也就是，行的系统版本号小于或等于事务的系统版本号），这样可以确保事务读取的行，要么是在事务开始前已经存在的，要么是事务自身插入或者修改过的。行的删除版本要么未定义，要么大于当前事务版本号。这可以确保事务读取到的行，在事务开始之前未被删除。

- INSERT

  InnoDB 为新插入的每一行保存当前系统版本号作为行版本号。

- DELETE

  InnoDB 为删除的每一行保存当前系统版本号作为行删除标识。

- UPDATE

  InnoDB 为插入一行新记录，保存当前系统版本号作为行版本号，同时保存当前系统版本号到原来的行作为行删除标识。

1. *删除操作到底有没有删除数据，腾出空间？*

   ？？

2. *更新操作有没有删除原来数据，腾出空间？*

   ？？

做实验验证一下。

### 2.6. 死锁

死锁是指两个或者多个事务再同一资源上相互占用，并请求锁定对方占用的资源，从而导致恶性循环的现象。当多个事务试图以不同的顺序锁定资源时，就可能会产生死锁。多个事务同事锁定相同的资源时，也会产生死锁。

InnoDB 目前处理死锁的方法是，将持有最少行级排他锁的事务进行回滚。

## 3. 索引背后的故事

### 3.1. 从问题入手

开始正文之前，大家可以思考几个问题：

1. 索引背后的数据结构是啥？
2. 查询与索引有什么基情？
3. 怎么优化查询，让它更加高效节能？

让我们带着下面这个问题，去看接下来的内容：

|      | 如何在一堆数据中查找某个数据？简单点，比如找出100以内的某个数。 |
| ---- | ------------------------------------------------------------ |

索引优化应该是对查询性能优化最有效的手段。可以轻松提高几个数量级。

创建一个真正的“最优”的索引经常需要重写查询。

常言道：知其然，知其所以然。学习一门技术的时候，不仅要学怎么使用，还要学习这门技术出现的背景是什么，是为了解决什么问题出现的，技术本身又有什么不足。这样才能更好地理解这门技术。所以，在正式开始讲解索引之前，让我们先看看索引出现的原因以及实现索引时使用的数据结构。

### 3.2. 追本溯源

计算机科学分为两块，**一块是硬件；另外，一块就是软件**。我们从这两方面说起。

计算机中，数据最后还是要落地到存储介质中。所以，我们需要了解一下计算机中的存储介质。

1984 年获得了图灵奖者瑞士计算机科学家尼克劳斯·威茨（Niklaus Wirth）提出一个著名公式 “算法 + 数据结构 = 程序”（Algorithm + Data Structures = Programs），简明扼要地说明了算法、数据结构及程序三者之间的关系。程序设计是一种智力劳动，算法与数据结构是编程之道中的“内功心法”，缺乏算法与数据结构素养，编程实践难以深入，也会限制码农利用计算机解决实际问题的能力。

我们先了解一下硬件相关的基础知识。

#### 3.2.1. 存储金字塔

计算机中最重要的存储介质分为几类：硬盘、内存、二级缓存、寄存器。它们之间的对比如下：

![image-20220123172155697](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123172155697-e0b7df.png)

图 7. 存储金字塔

从上面的图中，我们可以看出，**从下往上，速度从慢到快，制造成本也越来越高。**几种有代表性的存储设备的典型访问速度如下：

![image-20220123172209243](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123172209243-e49c46.png)

图 8. 存储访问时间

从这个图中，我们可以很明显的看出：**高速缓存的访问速度是主存的 10~100 倍，而主存的访问速度则是硬盘的 1～10W 倍。**

大概就是走路和坐飞机的差别了。虽然坐飞机是飞一样的感觉，但是走路还是我们最常用的移动方式。数据存储也一样，对于一台独立的计算机，数据最后还是要落地到磁盘上。所以，我们来看看机械硬盘的结构。

#### 3.2.2. 机械硬盘结构

机械硬盘中的大致结构如下图，类似很多电影和电视剧中的留声机：

![image-20220123172228699](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123172228699-f2cf92.png)

图 9. 机械硬盘单个盘面结构轮廓图

机械硬盘中，每一个磁盘盘面的组成结构如下：

![image-20220123172238691](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123172238691-e1d1ff-8de245.png)

图 10. 磁盘上的磁道、扇区和簇

英文名词解释：

- Spindle Motor 主轴马达
- Permanent Magnent 永久磁铁
- Voice Coil 音圈
- Head 磁头
- Spinning Hard Disk 旋转的硬盘

每个机械磁盘都有很多个盘面组成。整个机械磁盘的组成结构如下：

![image-20220123172324220](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123172324220-5848a9.png)

图 11. 磁盘内部结构

单词解释：

- spindle 转轴，主轴
- track 磁道
- sector 扇区
- cylinder 磁柱
- platter 磁盘
- head 磁头
- arm 磁臂
- 机械臂组件

- 寻道时间

  T-seek 是指将读写磁头移动至正确的磁道上所需要的时间。寻道时间越短，I/O操作越快，目前磁盘的平均寻道时间一般在 3－15ms。

- 旋转延迟

  T-rotation 是指盘片旋转将请求数据所在扇区移至读写磁头下方所需要的时间。旋转延迟取决于磁盘转速，通常使用磁盘旋转一周所需时间的 1/2 表示。比如，7200 rpm 的磁盘平均旋转延迟大约为 60 * 1000 / 7200 / 2 = 4.17ms，而转速为 15000 rpm 的磁盘其平均旋转延迟为 2ms。

- 数据传输时间

  T-transfer 是指完成传输所请求的数据所需要的时间，它取决于数据传输率，其值等于数据大小除以数据传输率。目前 IDE/ATA 能达到 133MB/s，SATA II 可达到 300MB/s 的接口数据传输率，数据传输时间通常远小于前两部分消耗时间。简单计算时可忽略。

**常见磁盘平均物理寻道时间为：**

- 7200 转/分的 STAT 硬盘平均物理寻道时间是 9ms
- 10000 转/分的 STAT 硬盘平均物理寻道时间是 6ms
- 15000 转/分的 STAT 硬盘平均物理寻道时间是 4ms

**常见硬盘的旋转延迟时间为：**

- 7200 rpm的磁盘平均旋转延迟大约为 60*1000/7200/2 = 4.17ms
- 10000 rpm的磁盘平均旋转延迟大约为 60*1000/10000/2 = 3ms，
- 15000 rpm的磁盘其平均旋转延迟约为 60*1000/15000/2 = 2ms。

了解磁盘读取数据的原理以各种延迟后，我们再来看看顺序读取和随机读取的差别：

![image-20220123172345646](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123172345646-0ec164.png)

图 12. 顺序读取和随机读取

因为机械硬盘的磁头移动至正确的磁道上需要时间，随机读写时，磁头不停的移动，时间都花在了磁头寻道上，导致的就是性能不高。所以，对于机械硬盘来说，连续读写性很好，但随机读写性能很差。具体对比如下：

![image-20220123172401620](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123172401620-61976c.png)

图 13. 对比在硬盘和内存上的随机读取和顺序读取

加州大学 Berkeley 分校统计的各种读取介质的延迟： [Numbers Every Programmer Should Know By Year](https://people.eecs.berkeley.edu/~rcs/research/interactive_latency.html)

#### 3.2.3. 局部性原理与磁盘预读

由于存储介质的特性，硬盘本身存取就比主存慢很多，再加上机械运动耗费，硬盘的存取速度往往是主存的几百分分之一，因此为了提高效率，要尽量减少磁盘 I/O。由于磁盘顺序读取的效率很高（不需要寻道时间，只需很少的旋转时间），因此对于具有局部性的程序来说，预读可以提高 I/O 效率。磁盘往往也不是严格按需读取，而是每次都会预读，即使只需要一个字节，磁盘也会从这个位置开始，顺序向后读取一定长度的数据放入内存。这样做的理论依据是计算机科学中著名的局部性原理：

**当一个数据被用到时，其附近的数据也通常会马上被使用。**

MySQL 在读取的时候，并不是每条每条读取，而是每次读取一页，一页通常包含好多条。

接下来，我们了解一下算法相关的背景知识。

|      | 我提到的问题：如何在一堆数据中查找某个数据？从这些硬件上来看，在内存中，甚至在一二三级高速缓存中，查找最快。当然，前提是，这些存储足够存得下。 |
| ---- | ------------------------------------------------------------ |

#### 3.2.4. 时间复杂度

时间复杂度用来检验某个算法处理一定量的数据要花多长时间。

重要的不是数据量，而是当数据量增加时运算如何增加。

![image-20220123195322133](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123195322133-4e1b52.png)

图 14. 时间复杂度变化

- 绿：O(1)
- 蓝：O(n)
- 红：O(\(log_{2}n\)) 即使在十亿级数量时也很低
- 粉：O(\(n^2\)) 快速膨胀

> **一些必要的知识点**
>
> 1 秒(s) = 1000 （103） 毫秒(ms) 
>
> ​          = 1000000 （106） 微秒(μs) 
>
> ​          = 1000000000 （109） 纳秒(ns)
>
> 对数计算公式
>
> \(log_{b}{a} = \frac{lna}{lnb}\) — 一般科学计算器都提供 \(ln{N}\) 的计算，可以通过这个公式来计算 \(log_{2}{N}\)。

数据量低时，O(1) 和 O(n2)的区别可以忽略不计。粗略计算，假设现在的计算机每秒可以处理 1* 109 条指令每秒。比如，你有个算法要处理2000条元素。

- O(1) 算法会消耗 1 次运算

- O(\(log_{2}n\)) 算法会消耗 7 次运算

  \(\frac{log_{2}(2*10^{3}) 条指令}{10^{9} 条指令/秒} = 1.10 * 10^{-8} 秒 = 11 纳秒\)

- O(n) 算法会消耗 2000 次运算

  \(\frac{2*10^{3} 条指令}{10^{9} 条指令/秒} = 2 * 10^{-6} 秒 = 2 微妙\)

- O(\(n*log_{2}n\)) 算法会消耗 14,000 次运算

  \(\frac{(2*10^3)*log_{2}(2*10^3) 条指令}{10^{9} 条指令/秒} = 2.19*10^{-5} 秒 = 21.9 微秒\)

- O(\(n^2\)) 算法会消耗 4,000,000 次运算

  \(\frac{(2*10^3)^{2} 条指令}{10^{9} 条指令/秒} = 4.00 * 10^{-3} 秒 = 4 毫秒\)

在数据量非常小的情况下，最快 4 毫秒，最慢也只有 11 纳秒。人类几乎感知不出什么差别。但是，如果处理 1,000,000 条元素（这对数据库来说也不算大）。

- O(1) 算法会消耗 1 次运算

- O(\(log_{2}n\)) 算法会消耗 14 次运算

  \(\frac{log_{2}10^{6} 条指令}{10^{9} 条指令/秒} = 1.99 * 10^{-8} 秒 = 19.9 纳秒\)

- O(n) 算法会消耗 1,000,000 次运算

  \(\frac{10^{6} 条指令}{10^{9} 条指令/秒} = 1 * 10^{-3} 秒 = 1 毫秒\)

- O(\(n*log_{2}n\)) 算法会消耗 14,000,000 次运算

  \(\frac{10^6*log_{2}10^{6} 条指令}{10^{9} 条指令/秒} = 1.99*10^{-2} 秒 = 19.9 毫秒\)

- O(\(n^2\)) 算法会消耗 1,000,000,000,000 次运算

  \(\frac{(10^6)^{2} 条指令}{10^{9} 条指令/秒} = 1.00 * 10^{3} 秒 = 1000 秒\)

**O(\(n^2\)) 与 O(\(n\*log_{2}n\)) 相差了 \(\frac{1.00 \* 10^{3}}{1.99\*10^{-2}} = 502512.56\) 倍。**我们把数据扩大到 10,000,000 条元素：

- O(1) 算法会消耗 1 次运算

- O(\(log_{2}n\)) 算法会消耗 23.25 次运算

  \(\frac{log_{2}10^{7} 条指令}{10^{9} 条指令/秒} = 2.33 * 10^{-8} 秒 = 23.3 纳秒\)

- O(n) 算法会消耗 10,000,000 次运算

  \(\frac{10^{7} 条指令}{10^{9} 条指令/秒} = 1 * 10^{-2} 秒 = 10 毫秒\)

- O(\(n*log_{2}n\)) 算法会消耗 232,500,000 次运算

  \(\frac{10^7*log_{2}10^{7} 条指令}{10^{9} 条指令/秒} = 2.33*10^{-1} 秒 = 0.233 秒\)

- O(\(n^2\)) 算法会消耗 100,000,000,000,000 次运算

  \(\frac{(10^7)^{2} 条指令}{10^{9} 条指令/秒} = 1.00 * 10^{5} 秒 = 27.78 小时\)

**O(\(n^2\)) 与 O(\(n\*log_{2}n\)) 相差了 \(\frac{1.00 \* 10^{5}}{0.233} = 429184.5\) 倍。**

这里可以明白：

- 搜索一个好的哈希表会得到 O(1) 复杂度
- 搜索一个均衡的树会得到 O(log(n)) 复杂度
- 搜索一个阵列会得到 O(n) 复杂度
- 最好的排序算法具有 O(n*log(n)) 复杂度
- 糟糕的排序算法具有 O(n2) 复杂度

>   我提到的问题：如何在一堆数据中查找某个数据？
>
> 在条件允许的情况下，我们应该选择时间复杂度尽量小的算法。

#### 3.2.5. 归并排序

合并排序基于这样一个技巧：将 2 个大小为 N/2 的已排序序列合并为一个 N 元素已排序序列仅需要 N 次操作。这个方法叫做合并。

![merge_sort](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/merge_sort-83cbae.gif)



图 15. 归并排序

这个算法有两点特别棒的优势：

- 可以更改算法，以便于同时使用磁盘空间和少量内存而避免巨量磁盘 I/O。方法是只向内存中加载当前处理的部分。在仅仅100MB的内存缓冲区内排序一个几个GB的表时，这是个很重要的技巧。
- 可以更改算法，以便于在多处理器/多线程/多服务器上运行。 分布式归并排序时 Hadoop 的关键组件之一。

#### 3.2.6. 二分查找

![binary_search_23](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/binary_search_23-5210df.gif)



图 16. 二分查找-最好情况

![binary_search](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/binary_search-1ce037.gif)

图 17. 二分查找-最坏的情况

|      | 我提到的问题：如何在一堆数据中查找某个数据？二分查找需要讲数组全部加载到内存中。但是，如果数据量特别大，加载不完，怎么办呢？能否只加载一部分数据呢？ |
| ---- | ------------------------------------------------------------ |

#### 3.2.7. 树

树，这种数据结构就能满足我们的需求，我们可以只把树的上面几级保存到内存中，方便操作。如下图：

![image-20220123195747991](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123195747991-371d93.png)

图 18. 树

树的节点也可以保持有序状态：

![image-20220123195802088](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123195802088-8fcbd2.png)

图 19. 搜索树

我们来看一下最简单的树结构。

|      | 我提到的问题：如何在一堆数据中查找某个数据？树能否保持有序呢？ |
| ---- | ------------------------------------------------------------ |

#### 3.2.8. 二叉查找树

在二叉查找树和在有序数组中查找某一个指定元素的对比如下：

![binaray_search_tree](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/binaray_search_tree-26b268.gif)



图 20. 二叉查找树

二叉查找树中每个节点要保证两点：

- 比保存在左子树的任何键值都要大
- 比保存在右子树的任何键值都要小

这个查询的成本是 log2(n)。

[二叉查找树在线演示](http://www.cs.usfca.edu/~galles/visualization/BST.html)

上面的是理想状况下的情况。但在极端情况下，二叉查找树的查询成本有可能是 n。例如：

![image-20220123195855820](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123195855820-b4abbd.png)

|      | 我提到的问题：如何在一堆数据中查找某个数据？能否能避免这种极端情况出现呢？ |
| ---- | ------------------------------------------------------------ |

#### 3.2.9. 平衡二叉查找树

![image-20220123195921532](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123195921532-4c759f.png)

图 22. 二叉搜索树对比

平衡二叉搜索树在添加元素时，通过旋转来保证自身的平衡性。

![image-20220123195941942](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123195941942-391eac.png)

图 23. 平衡二叉搜索树旋转

不仅能左旋，还可以右旋。左右旋转示意图：

![image-20220123200012262](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123200012262-61deda.png)

图 24. 二叉搜索树旋转

|      | 我提到的问题：如何在一堆数据中查找某个数据？对于查找一个特定值这种树挺好用。还有一个问题：如果查找一个范围内的值呢？比如年龄大于 16，小于 29 的美女呢？这个还可以枚举。如果不能枚举，怎么搞？ |
| ---- | ------------------------------------------------------------ |

#### 3.2.10. B+Tree

为了解决高效查找某一个范围内的元素的问题，我们引入一个修订后的树：B+树。这也是目前大部分现代数据库索引使用的数据结构。在一个B+树里：

- 只有最底层的节点（叶子节点）才保存信息（相关表的行位置）
- 其它节点只是在搜索中用来指引到正确节点的。

![image-20220123200048120](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123200048120-6c875c.png)



找到了 M 个后续节点，树总共有 N 个节点。对指定节点的搜索成本是 log(N)，跟上一个树相同。但是当你找到这个节点，你得通过后续节点的连接得到 M 个后续节点，这需要 M 次运算。那么这次搜索只消耗了 M+log(N) 次运算，区别于上一个树所用的 N 次运算。

|      | B+树种的 B 不是代表二叉（binary），而是代表平衡（balance），因为 B+树是从最早的平衡二叉树演化而来，但是 B+树不是一个二叉树。 |
| ---- | ------------------------------------------------------------ |

|      | 我提到的问题：如何在一堆数据中查找某个数据？有没有更快的查找算法呢？ |
| ---- | ------------------------------------------------------------ |

#### 3.2.11. 哈希表

为了构建一个哈希表，你需要定义：

- 元素的关键字
- 关键字的哈希函数。关键字计算出来的哈希值给出了元素的位置（叫做哈希桶）。
- 关键字比较函数。一旦你找到正确的哈希桶，你必须用比较函数在桶内找到你要的元素。

![image-20220123200144583](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123200144583-3d3587.png)

图 26. 哈希表

**真正的挑战是找到好的哈希函数，让哈希桶里包含非常少的元素。如果有了好的哈希函数，在哈希表里搜索的时间复杂度是 O(1)。**

|      | 我提到的问题：如何在一堆数据中查找某个数据？Hash查找有什么问题吗？ |
| ---- | ------------------------------------------------------------ |

### 3.3. InnoDB 逻辑存储结构

所有数据都被逻辑地存放在一个空间中，称为表空间（tablespace）。表空间由段（segment）、区（extent）、页（page）组成。页在一些文档中有时也被称为块（block）。大致结构如下：

![image-20220123200217181](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123200217181-3af62b.png)

图 27. InnoDB 逻辑存储结构

#### 3.3.1. 行

InnoDB 存储引擎是面向列的（row-oriented），也就是说数据是按行进行存放的。每个页存放的行记录是有硬性定义的，最多允许存放 16KB / 2-200 行的记录，即 7992 行记录。

### 3.4. 索引基础

索引类似书籍目录。

在MySQL 中，索引是在存储引擎层而不是服务器层实现的。

#### 3.4.1. 索引类型

##### 3.4.1.1. B-Tree 索引

大部分 MySQL 引擎都支持 B-Tree 索引。

NDB 集群存储引擎内部实际使用了 T-Tree 结构； InnoDB 则使用的是 B+Tree。

MyISAM 使用前缀压缩技术是索引更小；

MyISAM 索引通过数据的物理位置引用被索引的行，而 InnoDB 则根据逐渐引用被索引的行。

B-Tree 通常以为这所有的值都是按顺序存储的，并且每一个叶子页到根的距离相同。如下图：

![image-20220123200241165](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123200241165-f4800f.png)

图 28. B-Tree 索引结构

B-Tree 索引能够加快访问数据的速度，因为存储引擎不再需要进行全表扫描来获取需要的数据，取而代之的是从索引的根节点开始进行搜索。

![image-20220123200307309](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123200307309-b0a472.png)



|      | 问：索引的根节点的值变还是不变？ |
| ---- | -------------------------------- |

叶子节点比较特别，他们的指针指向的是被索引的数据，而不是其他的节点页。

树的深度和表的大小直接相关。

B-Tree 对索引列是顺序组织存储的，所以很适合查找范围数据。

例如：

```mysql
CREATE TABLE people (
  last_name  VARCHAR(50)     NOT NULL,
  first_name VARCHAR(50)     NOT NULL,
  dob        DATE            NOT NULL,
  gender     ENUM ('m', 'f') NOT NULL,
  KEY (last_name, first_name, dob)
);
```

三个列组成的联合索引的结构如下：

![image-20220123200356598](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123200356598-105772.png)

图 30. B-Tree 联合索引

注意：索引对多个值进行排序的依据是 `CREATE TABLE` 语句中定义索引时列的顺序。

B-Tree 索引有效的查询：

- 全值匹配

  全值匹配指的是和索引中的所有列进行匹配。

- 匹配最左前缀

  只使用索引前面的列。

- 匹配列前缀

  也可以只匹配某一列的值的开头部分。

- 匹配范围值

  比如只匹配名字

- 精确匹配某一列并范围匹配另外一列

  精确匹配第一列，范围匹配第二列。

- 只访问索引的查询

  查询只需要访问索引，而无须访问数据行。“覆盖索引”。

是因为索引树种的节点是有序的，除了查找之外，还可以用于查询中的 `ORDER BY` 操作。一般来说，**如果 B-Tree 可以按照某种方式查找到值，那么也可以按照这种方式用于排序。所以，如果 `ORDER BY` 子句满足前面列出的几种查询类型，则这个索引页可以满足对应的排序需求。**

B-Tree 索引的限制：

- 如果不是按照索引的最左列开始查找，则无法使用索引。
- 不能跳过索引中的列。
- 如果查询中有某个列的范围查询，则其右边所有列都无法使用索引优化查找。

再次提醒：索引列的顺序是多么重要，这些限制都和索引列的顺序有关。**在优化性能的时候，可能需要使用相同的列但顺序不同的索引来满足不同类型的查询需求。**

B+树索引并不能找到一个给定键值的具体行。B+树索引能找到的只是被查找数据行所在的页。然后数据库通过把页读入到内存，再在内存中进行查找，最后得到要查找的数据。

##### 3.4.1.2. 哈希索引

哈希索引（hash index）基于哈希表实现，只有精确匹配查询索引所有列的查询才有效。

在 MySQL 中，只有 Memory 引擎显式支持哈希索引。 Memory 引擎是支持 非唯一哈希索引的。

```mysql
CREATE TABLE hash_test (
  fname VARCHAR(50) NOT NULL,
  lname VARCHAR(50) NOT NULL,
  #  建立哈希索引的方式
  KEY USING HASH (fname) 
  # 指定引擎的方式
) ENGINE = MEMORY; 
```

如果多个列的哈希值相同，索引会以链表的方式存放多个记录指针到同一个哈希条目中。

哈希索引的限制：

- 哈希索引只包含哈希值和行指针，而不存储字段值，所以不能使用索引中的值来避免读取行。
- 哈希索引数据并不是按照索引值顺序存储的，所以也就无法用于排序。
- 哈希索引也不支持部分索引列匹配查找，因为哈希索引始终是使用索引列的全部内容来计算哈希值的。
- 哈希索引只支持等值比较查询，包括 `=`、 `IN()`、 `<⇒`(注意 `<>` 和 `<⇒` 是不同的操作)。
- 访问哈希索引的数据非常快，除非有很多哈希冲突。哈希冲突时使用链表来解决哈希冲突。
- 如果哈希冲突很多的话，一些所以维护操作的代价也会很高。冲突越多，代价越大。

因为这些限制，哈希索引只适用于某些特定的场合。而一旦适合哈希索引，则它带来的性能提升将非常显著。

除了 Memory 索引外，NDB 集群引擎也支持唯一哈希索引，且在 NDB 集群引擎中作用非常特殊。

InnoDB 引擎有一个特殊的功能叫“自适应哈希索引（adaptive hash index）”。当 InnoDB 注意到某些索引值使用得特别频繁时，它会在内存中基于 B-Tree 索引之上再创建一个哈希索引，这样就让 B-Tree 索引也具有哈希索引的一些优点，比如快速的哈希查找。这是一个完全自动的、内部的行为，用户无法控制或者配置，如有必要，可以关闭。

**创建自定义哈希索引**

如果存储引擎不支持哈希索引，可以模拟 InnoDB 一样创建哈希索引。思路：在 B-Tree 基础上创建一个伪哈希索引。并不是真正的哈希索引，本质还是使用 B-Tree 进行查找，但它使用哈希值而不是键本身进行查找。需要做的就是在查询的 `WHERE` 子句中手动指定使用哈希函数。

代码 4. 以 URL 列为例的自定义哈希索引

```mysql
SELECT id
FROM url
WHERE url='http://www.diguage.com/';

-- 创建自定义哈希索引
-- 注意：这里需要在 url_crc 字段上创建索引
SELECT id
FROM url
WHERE url='http://www.diguage.com/'
    AND url_crc=CRC32('http://www.diguage.com/');

-- 另外一种方式就是对完整的 URL 字符串做索引，那样会非常慢。

```

自定义哈希索引的缺陷是需要维护哈希值。可以手动维护，也可以使用触发器实现。示例如下：

代码 5. 基于触发器的自定义哈希索引

```mysql
DROP TABLE IF EXISTS url;
CREATE TABLE url (
  id      INT UNSIGNED NOT NULL AUTO_INCREMENT,
  url     VARCHAR(255) NOT NULL,
  url_crc INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  # 这个索引必须创建。
  KEY (url_crc)  
);


DELIMITER //

-- 插入触发器
CREATE TRIGGER url_crc_ins
BEFORE INSERT ON url
FOR EACH ROW BEGIN
  SET new.url_crc = crc32(new.url);
END;

-- 更新触发器
CREATE TRIGGER url_crc_upd
BEFORE UPDATE ON url
FOR EACH ROW BEGIN
  SET new.url_crc = crc32(new.url);
END;

INSERT INTO url (url) VALUES ('http:\/\/www.diguage.com/');

SELECT *
# 注意查看查询结果中的 `url_crc` 字段的值。
FROM url; 

UPDATE url
SET url = 'http:\/\/www.diguage.com'
WHERE id = 1;

SELECT *
FROM url; 

SELECT id
FROM url
WHERE url_crc = crc32('http:\/\/www.diguage.com/')
	# 为避免冲突问题，使用哈希索引查询时，必须在 `WHERE` 子句中包含常量值。
      AND url = 'http:\/\/www.diguage.com/';  
```

生日悖论，出现哈希冲突的概率的增长速度可能比想象的要快得多。

```mysql
SELECT
  CRC32('gnu'),
  CRC32('codding');

```

|      | 可以把哈希索引的实现原理对比 `HashMap` 的代码实现。 |
| ---- | --------------------------------------------------- |

采用这种方式，记住**不要使用 `SHA1()` 和 `MD5()` 作为哈希函数。**因为这两个函数计算出来的哈希值是非常长的字符串，会浪费大量空间，更新时也会更慢。 `SHA1()` 和 `MD5()` 设计目标是最大限度消除冲突，但这里并不需要这样高的要求。简单哈希函数的冲突在一个可以接受的范围，同时又能够提供更好的性能。

如果数据表非常大， `CRC32()` 会出现大量的哈希冲突，则可以实现一个简单的 64 位哈希函数。一个简单的办法可以使用 `MD5()` 函数返回值的一部分来作为自定义函数。性能稍差，但实现简单。

```mysql
SELECT CONV(RIGHT(MD5('http:\/\/www.diguage.com/'), 16), 16, 10) AS hash64;
```

##### 3.4.1.3. 空间数据索引（R-Tree）

MyISAM 表支持空间索引，可以用作地理数据存储。空间索引会从所有唯独来索引数据。查询时，可以有效地使用任意维度来组合查询。必须使用 MySQL 的 GIS 相关函数如 `MBRCONTAINS()` 等来维护数据。

开源关系数据库系统中对 GIS 的解决方案做得比较好的是 PostgreSQL 的 PostGIS。

##### 3.4.1.4. 全文索引

全文索引时一种特殊类型的索引，它查找的是文本中的关键词，而不是直接比较索引中的值。

全文索引更类似于搜索引擎做的事情，而不是简单的 `WHERE` 条件匹配。

全文索引适用于 `MATCH AGAINST` 操作，而不是普通的 `WHERE` 条件查询。

##### 3.4.1.5. 分形树索引（fractal tree index）

这是一类比较新开发的数据结构，既有 B-Tree 的很多优点，也避免了 B-Tree 的一些缺点。

### 3.5. 索引的优点

索引可以快速定位到表的指定位置；可以用作 `ORDER BY` 和 `GROUP BY` 操作；某些查询只使用索引就能够完成全部查询。

索引的三个有点：

1. 索引大大减少了服务器需要扫描的数据量。
2. 索引可以帮助服务器避免排序和临时表。
3. 索引可以将随机 I/O 变为顺序 I/O 。

关于索引推荐阅读 Tapio Lahdenmaki 和 Michael Leach 编写的 [数据库索引设计与优化](https://book.douban.com/subject/26419771/)，该书详细介绍了如何计算索引的成本和作用、如何评估查询速度、如何分析索引维护的代价和其带来的好处等。

Tapio Lahdenmaki 和 Michael Leach 在书中介绍了如何评价一个索引是否适合某个查询的“三星系统”（three-star system）：

1. 索引将相关的记录放到一起则获得一星；
2. 如果索引中的数据顺序和查找中的排列顺序一致则获得二星；
3. 如果索引中的列包含了查询中需要的全部列则获得“三星”。

> 索引时最好的解决方案吗？
>
> 索引不总是最好的工具。只有当索引帮助存储引擎快速查找到记录带来的好处大于其带来的额外工作时，索引才是有效的。对于非常小的表，大部分情况下简单全表扫描更高效。对于中到大型的表，索引就非常有效。但对于特大型的表，建立和使用索引的代价将随之增长。这时就需要分区技术。
>
> 如果表的数量特别多，可以建立一个元数据信息表，用于查询需要用到的某些特性。例如
>
> 对于 TB 级别的数据，定位单条记录的意义不大，所以需要经常会使用块级别元数据技术来替代索引。



### 3.6. 高性能的索引策略

正确地创建和使用索引时实现高性能查询的基础。

#### 3.6.1. 独立的列

“独立的列”是指索引列不能是表达式的一部分，也不能是函数的参数。

应该养成简化 `WHERE` 条件的习惯，始终将索引列单独放在比较符合的一侧。

代码 6. 对比独立列与

```mysql
USE sakila;

# 带数学计算的例子
EXPLAIN
SELECT actor_id
FROM actor
WHERE actor_id + 1 = 5 \G
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: actor
   partitions: NULL
         type: index
possible_keys: NULL
          key: idx_actor_last_name
      key_len: 182
          ref: NULL
         rows: 200
     filtered: 100.00
        Extra: Using where; Using index

# 独立列
EXPLAIN
SELECT actor_id
FROM actor
WHERE actor_id = 4 \G
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: actor
   partitions: NULL
         type: const
possible_keys: PRIMARY
          key: PRIMARY
      key_len: 2
          ref: const
         rows: 1
     filtered: 100.00
        Extra: Using index
```

#### 3.6.2. 前缀索引和索引选择性

当索引很长的字符列，会让索引变得大且慢，一个策略是前面提到过的模拟哈希索引。

通常可以索引开始的部分字符，可以大大节约索引空间，从而提高索引效率。但这样会降低索引的选择性。

索引的选择性是指，不重复的索引值（也称为基数，cardinality）和数据表的记录总数（#T）的比值，范围从 1/#T 到1之间。索引的选择性越高则查询效率越高，因为选择性高的索引可以让 MySQL 在查找时过滤掉更多的行。唯一索引的选择性是 1，这是最好的索引选择性，性能也是最好的。

一般情况下某个列前缀的选择性也是足够高的，足以满足查询性能。对于 `BLOB`、 `TEXT` 或者很长的 `VARCHAR` 类型的列，必须使用前缀索引。

诀窍在于要选择足够长的前缀以保证较高的选择性，同时又不能太长（以便节约空间）。前缀应该足够长，以是的前缀索引的选择性接近于索引整个列。换句话说，前缀的“基数”应该接近于完整列的“基数”。

为了觉得前缀的合适长度，需要找到最常见的值的列表，然后和最常见的前缀列表进行比较。

代码 7. 使用 SQL 语句来查看前缀长度的选择性

```mysql
USE sakila;

# 字符串长度统计
SELECT
  CHAR_LENGTH(city) AS len,
  count(*)          AS cnt
FROM city
GROUP BY len
ORDER BY len DESC;

+-----+-----+
| len | cnt |
+-----+-----+
|  26 |   3 |
|  23 |   4 |
|  22 |   2 |
|  21 |   2 |
|  20 |   4 |
|  19 |   5 |
|  18 |   4 |
|  17 |   7 |
|  16 |   6 |
|  15 |   9 |
|  14 |   8 |
|  13 |   8 |
|  12 |  15 |
|  11 |  29 |
|  10 |  45 |
|   9 |  61 |
|   8 |  88 |
|   7 |  95 |
|   6 | 107 |
|   5 |  56 |
|   4 |  35 |
|   3 |   6 |
|   2 |   1 |
+-----+-----+


# 字符串选择性
SELECT
  COUNT(DISTINCT LEFT(city, 2)) / COUNT(*) AS cit2,
  COUNT(DISTINCT LEFT(city, 3)) / COUNT(*) AS cit3,
  COUNT(DISTINCT LEFT(city, 4)) / COUNT(*) AS cit4,
  COUNT(DISTINCT LEFT(city, 5)) / COUNT(*) AS cit5,
  COUNT(DISTINCT LEFT(city, 6)) / COUNT(*) AS cit6,
  COUNT(DISTINCT LEFT(city, 7)) / COUNT(*) AS cit7,
  COUNT(DISTINCT LEFT(city, 8)) / COUNT(*) AS cit8,
  COUNT(DISTINCT city) / COUNT(*)          AS city
FROM city;

+--------+--------+--------+--------+--------+--------+--------+--------+
| cit2   | cit3   | cit4   | cit5   | cit6   | cit7   | cit8   | city   |
+--------+--------+--------+--------+--------+--------+--------+--------+
| 0.3133 | 0.7633 | 0.9383 | 0.9750 | 0.9900 | 0.9933 | 0.9933 | 0.9983 |
+--------+--------+--------+--------+--------+--------+--------+--------+

# 再对比一下不同长度字符的分布情况
SELECT
  count(*)      AS cnt,
  left(city, 2) AS pref
FROM city
GROUP BY pref
ORDER BY cnt DESC; 
# 结果集太多，不再展示。

SELECT
  count(*)      AS cnt,
  left(city, 6) AS pref
FROM city
GROUP BY pref
ORDER BY cnt DESC;
# 结果集太多，不再展示。
```

根据统计，我们只需要针对前六个字符建立前缀索引即可：

代码 8. 建立前缀索引

```
CREATE INDEX idx_city_pre6
  ON city (city(10)); 

# 或
ALTER TABLE city
  ADD KEY (city(6));
```

>  注意：这里只取了 `city` 列前六个字符来建立索引。

前缀索引时一种能使索引更小、更快的有效办法；也有缺点，**MySQL 无法使用前缀索引做 `ORDER BY` 和 `GROUP BY`，也无法使用前缀索引做覆盖索引**。

一个常见的场景是针对很长的十六进制唯一 ID 使用前缀索引。例如 SessionID。

>   有时后缀索引(suffix index)也有用途。 MySQL 原生不支持反向索引，但可以把字符串反转后存储，并基于此建立前缀索引。可以通过触发器来维护这种索引。

#### 3.6.3. 多列索引

一个常见的错误就是，为每个列创建独立的索引，或者按照错误的顺序创建多列索引。

在多个列上山里独立的单列索引大部分情况下并不能提高 MySQL 的查询性能。 MySQL 5.0 和更新版本引入了一种“索引合并”（index merge）的策略，一定程度上可以使用表上的多个单列索引来定位指定的行。

代码 9. 索引合并

```mysql
# 不支持索引合并就需要做全表扫描
SELECT
  film_id,
  actor_id
FROM film_actor
WHERE film_id = 1 OR actor_id = 1;

# 在支持索引合并前，只能这样优化
EXPLAIN
SELECT
  film_id,
  actor_id
FROM film_actor
WHERE actor_id = 1
UNION ALL
SELECT
  film_id,
  actor_id
FROM film_actor
WHERE film_id = 1 AND actor_id <> 1 \G
*************************** 1. row ***************************
           id: 1
  select_type: PRIMARY
        table: film_actor
   partitions: NULL
         type: ref
possible_keys: PRIMARY
          key: PRIMARY
      key_len: 2
          ref: const
         rows: 19
     filtered: 100.00
        Extra: Using index
*************************** 2. row ***************************
           id: 2
  select_type: UNION
        table: film_actor
   partitions: NULL
         type: range
possible_keys: PRIMARY,idx_fk_film_id
          key: idx_fk_film_id
      key_len: 4
          ref: NULL
         rows: 10
     filtered: 100.00
        Extra: Using where; Using index


# 支持索引合并后
EXPLAIN
SELECT
  film_id,
  actor_id
FROM film_actor
WHERE film_id = 1 OR actor_id = 1 \G
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: film_actor
   partitions: NULL
         type: index_merge
possible_keys: PRIMARY,idx_fk_film_id
          key: idx_fk_film_id,PRIMARY
      key_len: 2,2
          ref: NULL
         rows: 29
     filtered: 100.00
        Extra: Using union(idx_fk_film_id,PRIMARY); Using where

```

索引合并测试有时候是一种优化的结果，但**实际上更多时候说明了表上的索引建的很糟糕**：

- 当出现服务器对多个索引做相交操作时（通常有多个 `AND` 条件），通常意味着需要一个包含所有相关列的多列索引，而不是多个独立的单列索引。
- 当服务器需要多多个索引做联合操作时（通常有多个 `OR` 条件），通常需要耗费大量 CPU 和内存资源在算法的缓存、排序和合并操作上。特别是当有些索引的选择性不高，需要合并扫描返回的大量数据的时候。
- 更重要的是，优化器不会把这些计算的“查询成本”中，优化器只关心随机页面读取。这使得查询的成本被“低估”。

如果在 `EXPLAIN` 中看到有索引合并，应该好好检查一下查询和表的结构，看是不是已经是最优的。

#### 3.6.4. 选择合适的索引列顺序

最容易引起困惑的问题就是索引列的顺序。正确的顺序依赖于使用该索引的查询，并且同时需要考虑如何更好地满足排序和分组的需要。本节内容适用于 B-Tree 索引。

在一个多列 B-Tree 索引中，索引列的顺序意味着索引首先按照最左列进行排序，其次是第二列，以此类推。所以，索引可以按照升序或者降序进行扫描，以满足精确符合列顺序的 `ORDER BY`、 `GROUP BY` 和 `DISTINCT` 等子句的查询需求。

在 Lahdenmaki 和 Leach 的[“三星索引”](https://notes.diguage.com/mysql/#three-star-system)系统中，列顺序也决定了一个索引是否能够成为一个真正的“三星索引”。

对于如何选择索引的列顺序有一个经验法则：**将选择性最高的列放到索引最前列。**通常不如避免随机 IO 和排序那么重要。

当不需要考虑排序和分组时，将选择性最高的列放到索引最前列通常是很好的。

>   这就是在思考建立联合索引时的一个指导原则！选择方法如下：
>
> ```mysql
> USE sakila; 
> # 这里使用了 MySQL 官方提供的 sakila 示例数据库。
> SELECT
>   sum(staff_id = 2),
>   sum(customer_id = 584)
> FROM payment;
> ```
>
> 根据执行结果，结合上面提到的指导原则，应该讲结果值更小的列放在前面。
>
> 这里有个地方需要注意：上面查询的结构非常依赖于选定的具体值。对其他查询可能就不适用。
>
> 经验法则考虑的是全局基数和选择性，而不是某个具体查询。
>
> ```mysql
> USE sakila;
> 
> SELECT
>   COUNT(DISTINCT staff_id) / COUNT(*)    AS staff_id_selectivity,
>   COUNT(DISTINCT customer_id) / COUNT(*) AS customer_id_selectivity,
>   COUNT(*)
> FROM payment;
> ```
>
> 根据执行结构，选择数字比较高的列作为索引列的第一列。

性能不只是依赖于所有索引列的选择性（整体基数），也和查询条件的具体值有关，也就是和值的分布有关。

可能需要根据那些运行效率最高的查询来调整索引列的顺序。

尽管关于选择性和基数的经验法则值得去研究和分析，但一定要记住别忘了 `WHERE` 子句中的排序、分组和范围条件等其他因素，这些因素可能对查询的性能早晨非常大的影响。

#### 3.6.5. 聚簇索引

聚簇索引并不是一种单独的索引类型，而是一种数据存储方式。InnoDB 的聚簇索引实际上在同一结构中保存了 B-Tree 索引和数据行。

当表有聚簇索引时，它的数据行实际上存放在索引的叶子页（leaf page）中。术语“聚簇”表示数据行和相邻的键值紧凑地存储在一起。因此，一个表只有一个聚簇索引（不过，覆盖索引可以模拟多个聚簇索引的情况）。

![image-20220123201010282](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123201010282-174443-e4ad64.png)

图 31. 聚簇索引的数据分布

InnoDB 通过主键聚集数据。如果没有定义主键， InnoDB 会选择一个唯一的非空索引代替；如果没有这样的索引， InnoDB 会隐式定义哥主键来作为聚簇索引。

聚集的数据的一些重要的优点：

- 可以把相关数据保存在一起。例如，根据用户ID来聚集数据，可以顺序读取某个用户的全部邮件。
- 数据访问更快。聚簇索引将索引和数据保存在同一个 B-Tree 中，因此从聚簇索引中获取数据通常比非聚簇索引中查找要快。
- 使用覆盖索引扫描的查询可以直接使用页节点中的主键值。

聚集数据的一些缺点：

- 聚簇数据最大限度提高了 I/O 密集型应用的性能，但如果数据全部都放在内存中，则访问的顺序就没那么重要了，聚簇索引也就没什么优势了。
- 插入速度严重依赖于插入顺序。按照主键的顺序插入是加载数据到 InnoDB 表中速度最快的方式。但如果不是按照主键顺序加载数据，那么在加载完成后最好使用 `OPTIMIZE TABLE` 命令重新组织一下表。
- 更新聚簇索引列的代价很高，因为会强制 InnoDB 将每个被更新的行移动到新的位置。
- 基于聚簇索引的表在插入新行，或者主键被更新导致需要移动行的时候，可能面临“页分裂”的问题。页分裂会导致表占用更多的磁盘空间。
- 聚簇索引可能导致全表扫描变慢，尤其是行比较稀疏，或者由于页分裂导致数据存储不连续的时候。
- 二级索引（非聚簇索引）可能给想象的要更大，因为在二级索引的叶子节点包含了引用行的主键列。
- 二级索引访问需要两次索引查找，而不是一次。

二级索引叶子节点保存的不是指向行的物理位置的指针，而是行的主键值。二级索引要两次 B-Tree 查找而不是一次，对于 InnoDB，自适应哈希索引能够减少这样的重复工作。*为什么能减少？*

##### 3.6.5.1. InnoDB 和 MyISAM 的数据分布对比

为了方便讲解，分别使用 InnoDB 和 MyISAM 引擎建立结构如下的表，并按主键随机顺序插入主键值在 1 ~ 10000 的10000条数据：

```mysql
CREATE TABLE layout_test (
  col1 INT NOT NULL,
  col2 INT NOT NULL,
  PRIMARY KEY (col1),
  KEY (col2)
);
# 请在建立的时候指定引擎类型
```

**MyISAM 的数据分布**

MyISAM 按照数据插入的顺序存储在磁盘上。如图：

![image-20220123201205517](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123201205517-81726a.png)



图 32. MyISAM 表 layout_test 的数据分布

在行旁边显示了行号，从 0 开始递增。因为行是定长的，所以 MyISAM 可以从表的开头跳过所需要的字节找到需要的行。（MyISAM 是根据定长还是变长的行使用不同策略来确定行号。）

![image-20220123201800229](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123201800229-c82b93.png)

图 33. MyISAM 表 layout_test 的主键索引分布

这里有两点需要注意：

1. 主键叶子节点存放的指向数据行的指针。
2. 主键和其他索引没有什么区别。



![image-20220123201826331](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123201826331-97ae2b.png)

图 34. MyISAM 表 layout_test 的主键索引分布

![MyISAM 表 layout_test 的二级索引分布](https://notes.diguage.com/mysql/assets/images/MyISAM_secondary_key_layout.png)

图 35. MyISAM 表 layout_test 的二级索引分布

事实上， MyISAM 中主键索引和其他索引在结构上没有什么不同。主键索引就是一个名为 PRIMARY 的唯一非空索引。

**InnoDB 的数据分布**

InnoDB 支持聚簇索引，所以使用不同的方式存储同样的数据。

![image-20220123201853972](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123201853972-c0269b.png)



图 36. InnoDB 表 layout_test 的主键索引分布

注意：该图显示了整个表，而不是只有索引。在 InnoDB 中，聚簇索引“就是”表。

聚簇索引的每一个叶子节点都包含了主键值、事务 ID、用于事务和 MVCC 的回滚指针以及所有的剩余列。如果主键是一个列前缀索引， InnoDB 也会包含完整的主键列和剩下的其他列。

![image-20220123201918240](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123201918240-f72e29.png)



图 37. InnoDB 表 layout_test 的主键索引分布

|      | 前文说 InnoDB 把 `BLOB` 类型的会放在单独区域，如果主键是 `BLOB` 类型的列前缀索引，该如何存储？ |
| ---- | ------------------------------------------------------------ |

InnoDB 的二级索引和聚簇索引很不相同。 InnoDB 二级索引的叶子节点存储的不是“行指针”，而是主键值，并以此作为指向行的“指针”。这样的策略减少了当出现行移动或者数据页分裂时二级索引的维护。使用主键值当做指针会让二级索引占用更多的空间，换来的好处是， InnoDB 在移动行时无须更新二级索引中的这个“指针”。

|      | 对比来看， MyISAM 在更新时，如果出现行移动，则要更新所有的二级索引的行指针。 |
| ---- | ------------------------------------------------------------ |

![image-20220123201945484](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123201945484-28ae70.png)





图 38. InnoDB 表 layout_test 的二级索引分布

注意两点：

1. 每个叶子节点都包含了索引列，紧接着是主键索引。
2. 非叶子节点包含了索引列和一个指向下级节点的指针。这对聚簇索引和二级索引都是用。

![image-20220123202019755](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123202019755-3d7a4a.png)



图 39. 聚簇和非聚簇表对比

##### 3.6.5.2. 在 InnoDB 表中按主键顺序插入行

保证数据行是按顺序写入，对于根据主键做关联操作的性能也会更好。

最好避免随机的（不连续且值的分布范围非常大）聚簇索引，特别是对于 I/O 密集型的应用。随机主键使得聚簇索引的插入变得完全随机，这是最坏的情况，使得数据没有任何聚集特性。

![image-20220123202038823](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123202038823-b6c85f.png)

图 40. 向聚簇索引插入顺序的索引值

因为主键的值时顺序的，所以 InnoDB 把每一条记录都存储在上一条记录的后面。当达到页的最大填充因子时（InnoDB 默认的最大填充因子是页大小的 15/16，留出部分空间用于以后修改），下一条记录都会写入新的页中。一旦数据按照这种顺序的方式加载，主键页就会近似于被顺序的记录填满。

![image-20220123202059100](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123202059100-0e5241.png)

图 41. 向聚簇索引插入无序的索引值

因为主键值不一定比之前插入的大，所以 InnoDB 无法简单地总是把新行插入到索引的最后，而是需要为新的行寻找合适的位置 — 通常是已有数据的中间位置 — 并且分开空间。这会增加很多额外的工作，并导致数据分布不够优化。缺点：

- 写入的目标页可能已经刷到磁盘上并从缓存中移除，或者是还没有被加载到缓存中， InnoDB 在插入之前不得不先找到并从磁盘读取目标页到内存中。这将导致大量的随机 I/O。
- 因为写入是乱序的， InnoDB 不得不频繁地做页分裂操作，以便为新的行分配空间。页分裂会导致移动大量数据，一次插入最少需要修改三个页而不是一个页。 *为什么最少是三个页？*
- 由于频繁的页分裂，页会变得稀疏并被不规则地填充，所以最终数据会有碎片。

在把随机值载入到聚簇索引以后，也许需要做一次 `OPTIMIZE TABLE` 来重建表并优化页的填充。

> 顺序主键也会造成更坏的结果
>
> 对于高并发工作负载，在 InnoDB 中按主键顺序插入可能会造成明显的争用。主键的上界会成为“热点”。并发插入可能导致间隙锁竞争。另一个热点可能是 `AUTO_INCREMENT` 锁机制。



|      | 有一个经常在面试中被问到的问题：为什么索引比较多的情况下，插入、更新、删除都比较慢？可否只从索引中取数据而不回表？ |
| ---- | ------------------------------------------------------------ |

#### 3.6.6. 覆盖索引

设计优秀的索索引应该考虑到整个查询，而不单单是 `WHERE` 条件部分。

如果一个索引包含（或者说覆盖）所有需要查询的字段的值，则称之为“覆盖索引”。

覆盖索引时非常有用的工具，能够极大地提高性能。优点如下：

- 索引条目通常远小于数据行大小，所以如果只需要读取索引，则 MySQL 就会极大地减少数据访问量。
- 因为索引时按照列值顺序存储的（至少在单个页内是如此），所以对于 I/O 密集型的范围查询会比随机从磁盘读取每一行数据的 I/O 要少得多。
- 一些存储引擎如 MyISAM 在内存中只缓存索引，数据则依赖于操作系统来缓存，因此要访问数据需要一次系统调用。这可能会导致严重的性能问题。
- 由于 InnoDB 的聚簇索引，覆盖索引对 InnoDB 表特别有用。如果二级主键能够覆盖查询，则可以避免对主键索引的二次查询。

不是所有的索引都可以成为覆盖索引。覆盖索引必须要存储索引列的值，而哈希索引、空间索引和全文索引等都不存储索引列的值，所以 MySQL 只能使用 B-Tree 索引做覆盖索引。也不是所有的存储引擎都支持覆盖索引，比如 Memory 不支持。

索引覆盖查询还有很多陷阱可能会导致无法实现优化。 MySQL 查询优化器会在执行查询前判断是否有一个索引能进行覆盖。

|      | 这里思考一下，什么样的查询才是覆盖索引？需要满足什么条件？从 SQL 语句的组成来看。 |
| ---- | ------------------------------------------------------------ |

从下面的查询来看：

```mysql
SELECT *
FROM products
WHERE actor = 'SEAN CARREY'
      AND title LIKE '%APOLLO%';

```

这里索引无法覆盖该查询，有两个原因：

- 没有任何索引能够覆盖这个查询。查询从表中选择了所有的行，而没有任何索引覆盖了所有的列。
- MySQL 不能在索引中执行 `LIKE` 操作。这是底层存储引擎 API 的限制。MySQL 能在索引中做最左前缀匹配的 `LIKE` 比较。

可以重新查询并巧妙地设计索引，先将索引扩展至覆盖三个数据列（actor、title、prod_id），然后如下方式重写查询：

```mysql
SELECT *
FROM products
  JOIN (SELECT prod_id
        FROM products
        WHERE actor = 'SEAN CARREY'
              AND title LIKE '%APOLLO%') AS t1
    ON t1.prod_id = products.prod_id;

```

这种方式叫做延迟关联（deferred join），因为延迟了对列的访问。在查询的第一阶段 MySQL 可以使用覆盖索引，在 `FROM` 子句的子查询中找到匹配的 `prod_id`，然后根据这些 `prod_id` 值在外层查询匹配获取需要的所有列值。

这种优化方式在数据量很大，符合条件的数据很小时，优化效果明显；在数据量很大，符合条件的数据很大时，效果不明显，因为大部分时间是花在读取和发送数据了；如果数据量很小，子查询反而会拖慢查询。

|      | 以前觉得写 SQL 语句就是个技术活，现在来看，它还是一门艺术，一门需要思考的艺术！ |
| ---- | ------------------------------------------------------------ |

这里还有一点需要特别点出： InnoDB 的二级索引中还存放的是指向数据行的主键 ID。所以，除了索引列外，还有主键 ID 也可以在覆盖索引中使用。

未来 MySQL 版本的改进

上面提到限制主要是因为存储引擎 API 不允许 MySQL 将过滤条件传到存储引擎层导致的。MySQL 5.6 中包含了在存储引擎 API 上所做的一个重要的改进，其被称为“索引条件推送”（index condition pushdown），可以大大改善现在的查询执行方式，如此一来上面介绍的很多技巧也就不再需要了。

#### 3.6.7. 使用索引扫描来做排序

MySQL 有两种方式可以生成有序的结果：通过排序操作；或者按索引顺序扫描。

MySQL 可以使用同一个索引既满足排序，又用于查找行。设计索引时应该尽可能地同时满足这两种任务。

只有当索引的列顺序和 `ORDER BY` 子句的顺序完全一致，并且所有列的排序方向（倒序或正序）都一样时， MySQL 才能够使用索引来对结果做排序。如果查询需要关联多张表，则只有当 `ORDER BY` 子句引用的字段全部为第一个表时，才能使用索引做排序。 `ORDER BY` 子句和查找型查询的限制是一样的：需要满足索引的最左前缀的要求；否则， MySQL 都需要执行排序操作，而无法利用索引排序。

|      | 如果需要安装不同方向做排序，一个技巧是存储该列值的反转串或者相反数。 |
| ---- | ------------------------------------------------------------ |

还有一种情况下 `ORDER BY` 子句可以不满足索引的最左前缀的要求，就是前导列为常量的时候。可以在 `WHERE` 子句或者 `JOIN` 子句中对这些列指定了常量，就可以 “弥补” 索引的不足。

使用索引做排序的一个最重要的用法是当查询同时有 `ORDER BY` 和 `LIMIT` 子句的时候。

#### 3.6.8. 压缩（前缀压缩）索引

MyISAM 使用前缀压缩来减少索引的大小，可让更多索引放入内存中，某些情况可以极大提高性能。默认只压缩字符串，通过参数设置可以对整数做压缩。

MyISAM 压缩每个索引块的方法是，先完全保存索引块中的第一个值，然后将其他值和第一个值进行比较得到相同前缀的字节数和剩余的不同后缀部分，把这部分存储起来即可。

压缩块使用更少的空间，代价是某些操作可能更慢。 MyISAM 查找时无法再索引块使用二分查找而只能从头开始扫描。正序的扫描速度还不错，但是如果是倒序扫描，就惨了！

对于 CPU 密集型应用，压缩使得 MyISAM 在索引查找上要慢好几倍。

可以在 `CREATE TABLE` 语句汇总指定 `PACK_KEYS` 参数来控制索引压缩的方式。

#### 3.6.9. 冗余和重复索引

重复索引指在相同的列上按照相同的顺序创建的相同类型的索引。

MySQL 的唯一限制和主键限制都是通过索引实现的。

冗余索引和重复索引有一些不同。如果创建了索引（A，B），再创建索引（A）就是冗余索引。

还有一种情况是将一个索引扩展为（A，ID），其中 ID 是主键，对于 InnoDB 来说主键列已经包含在二级索引中，这也是冗余。

大多数情况下都不需要冗余索引，应该尽快扩展已有的索引而不是创建新索引。但有时处于性能的考虑需要冗余，因为扩展已有的索引会导致其变得太大，从而影响其他使用该索引的查询的性能。

有时为了覆盖查询，也需要扩展索引。

一般来说，增加新索引将会对导致 `INSERT`、 `UPDATE`、 `DELETE` 等操作的速度变慢，特别是当新增加索引后导致达到了内存瓶颈的时候。

解决冗余索引和重复索引的方法很简单，删除这些索引即可，但首先要做的是找出这样的索引。

在决定哪些索引可以被删除的时候要非常小心。要考虑查询、排序等。可以使用 Percona 工具箱中的 `pt-upgrade` 工具来检查计划中的索引变更。

#### 3.6.10. 未使用的索引

除了冗余索引和重复索引，可能还会有一些服务器永远不用的索引，完全是累赘，建议考虑删除。

- 最简单有效的办法是在 Percona Server 或者 MariaDB 中先打开 `userstates` 变量，让服务器运行一段时间，再通过查询 `INFORMATION_SCHEMA.INDEX_STATISTICS` 就能查到每个索引的使用频率。
- Percona Toolkit 的 `pt-index-usage` 读取查询日志，并对日志中的查询进行 `EXPLAIN` 查找，然后打印出关于索引和查询的报告。

#### 3.6.11. 索引和锁

索引可以让查询锁定更少的行。锁定超过需要的行会增加锁争用并减少并发性。

InnoDB 只有在访问行的时候才会对其加锁，而索引能够减少 InnoDB 访问的行数，从而减少锁的数量。

InnoDB 在二级索引上使用共享（读）锁，但访问主键索引需要排他（写）锁。

### 3.7. 三星索引实战

#### 3.7.1. 定义

1. **如果与一个查询相关的索引行是相邻的，或者至少相距足够靠近的话**，那这个索引就可以被标记上第一颗星。*这最小化了必须扫描的索引片的宽度。*
2. **如果索引行的顺序与查询语句的需求一致**，则索引可以被标记上第二颗星。*这排除了排序操作。*
3. **如果索引行包含查询语句中的所有列**，那么索引就可以被标记上第三颗星。完全符合三星就是“覆盖索引”。将一个列排除在索引之外可能会导致许多速度较慢的磁盘随机读。

#### 3.7.2. 实践出真知

现在有表如下：

代码 10. 建表语句

```mysql
USE  sakila;

DROP TABLE cust;

CREATE TABLE `cust` (
  `cust_id` smallint(5) unsigned NOT NULL AUTO_INCREMENT,
  `first_name` varchar(45) NOT NULL,
  `last_name` varchar(45) NOT NULL,
  `email` varchar(50) DEFAULT NULL,
  `city_id` smallint(5) unsigned NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `create_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_update` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`cust_id`),
  KEY `idx_last_name_first_name` (`last_name`,`first_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO cust (first_name, last_name, email, city_id, active)
  SELECT
    first_name,
    last_name,
    email,
    address_id,
    active
  FROM customer;

UPDATE cust
SET last_name = 'CABRAL'
WHERE cust_id > 550;
```

如下查询是否符合三星索引的标准：

代码 11. 查询语句

```mysql
SELECT
  cust_id,
  first_name
FROM cust
WHERE last_name = 'CHOATE'
      AND city_id = 499
ORDER BY first_name;

# 查询已有的索引
SHOW INDEX FROM cust;

```

- 为了满足第一颗星

  取出所有等值谓词的列（`Where col=…`） ，把这些列作为索引最开头的列 — 以任意顺序都可以。针对上面的查询，可选的索引字段为：`(last_name, city_id)` 或 `(city_id, last_name)`。这样可以将索引片宽度缩减到最窄。

- 为了满足第二颗星

  将 `ORDER BY` 列加入到索引中。不要改变这些列的顺序，但是忽略哪些在第一步中已经加入索引的列。针对上面的查询，增加字段 `first_name`，可选索引字段变为：`(last_name, city_id, first_name)` 或 `(city_id, last_name, first_name)`。

  瓜哥注：

  针对这个查询来说，加入 `first_name` 字段，结果集中的记录就是有序的。因为通过 `last_name = 'CHOATE' AND city_id = 499` 而言，可以唯一确定紧挨着的一段数据。那么排序性就“传导”到了第三个字段 `first_name` 字段上去。在其他类型中，比如 `city_id > 500` 而言，则是“小范围有序，大范围无序”。则需要排序才能保证有序性。

- 为了满足第三颗星

  将查询语句中剩余的列加到索引中去，列在索引中添加的顺序对查询语句的性能没有影响，但是将**易变的列**放在最后能够降低更新的成本。最后，针对上面这个查询，整个查询中，只剩下 `cust_id`，加入索引字段，可选索引字段变为：`(last_name, city_id, first_name, cust_id)` 或 `(city_id, last_name, first_name, cust_id)`。

根据上面的分析，我们得到了两个可选项：`(last_name, city_id, first_name, cust_id)` 或 `(city_id, last_name, first_name, cust_id)`。那么，我们改如何选择呢？

前面的 [选择合适的索引列顺序](https://notes.diguage.com/mysql/#choose-index-fields-sequence) 中，提到了如何选择索引列顺序的一条经验法则：**将选择性最高的列放到索引最前列。**在不考虑其他业务，只关注当前查询SQL的情况下，我们可以遵循这条法则。前文 [选择合适的索引列顺序](https://notes.diguage.com/mysql/#choose-index-fields-sequence) 提到一个确定字段选择性的示例 SQL，这里修改如下：

代码 12. 查看字段选择性

```mysql
SELECT
  count(DISTINCT last_name) / count(*),
  count(DISTINCT city_id) / count(*)
FROM cust;

# 结果如下：
+--------------------------------------+------------------------------------+
| count(DISTINCT last_name) / count(*) | count(DISTINCT city_id) / count(*) |
+--------------------------------------+------------------------------------+
|                               0.9182 |                             1.0000 |
+--------------------------------------+------------------------------------+

```

我们只需要根据这里的结构，选择数字最大的字段在前面即可。根据结果，更合适的索引序列为：`(city_id, last_name, first_name, cust_id)`。

|      | **对于可以“任意顺序皆可”的列，两个法则可以遵循**将选择性更好的放在前面；如果选择性一样好，将稳定、不易变的列，放在前面，这样在修改时，移动的距离更短 |
| ---- | ------------------------------------------------------------ |

**结合 MySQL InnoDB 引擎的自身特性，`(city_id, last_name, first_name, cust_id)` 是最佳方案吗？为什么？**

#### 3.7.3. 范围谓词与三星索引

下面，来看一下带有范围谓词的示例：

代码 13. 范围谓词查询示例

```mysql
SELECT
  cust_id,
  first_name
FROM cust
WHERE last_name BETWEEN 'ADAMS' AND 'DANIELS'
      AND city_id = 580
ORDER BY first_name;
```

这个查询该如何建立“三星索引”呢？

1. 首先是最简单的星，第三颗星。确保查询语句中的所有列都在索引中就能满足第三颗星：`{city_id, last_name, cust_id, first_name}`
2. 第二，添加 `ORDER BY` 列 `first_name` 能使索引满足第二颗星。但是，前提是必须放在 `BETWEEN` 谓词列 `last_name` 前面才行。如果 `ORDER BY` 列 `first_name` 放在 `BETWEEN` 谓词列 `last_name` 后面，则索引不是按照 `first_name` 排序，因此需要排序操作。因此，为满足第二颗星， `ORDER BY` 列 `first_name` 必须放在 `BETWEEN` 谓词列 `last_name` 前面。如：`(first_name……)` 或 `(city_id, first_name……)` 等
3. 第三，考虑第一颗星。如果 `city_id` 是索引第一列，将会有一个相对比较窄的索引片需要扫描。（当然，这取决于 `city_id` 的选择性。）如果用 `(city_id, last_name……)` 的话，索引片更窄。那么，其他列（例如 `first_name`）就不能放在这两列之间。

综上，理想索引会有几颗星呢？首先，它一定能有第三颗星。其次，只能有第一颗星或第二颗星，不能同时拥有两者。换句话说，我们只能二选一：

- 避免排序 — 拥有第二颗星；
- 拥有可能最窄索引片，减少索引以及读取行数，拥有第一颗星。

具体选择，就要看业务需求。

#### 3.7.4. 设计最佳索引的算法

##### 3.7.4.1. 候选A — 选择最窄的索引片

1. 取出对于优化器来说不算过分复杂的**等值谓词列**，作为索引的前导列 — 以任意顺序皆可。
2. 将选择性最好的范围谓词作为索引的下一列，如果存在的话；
3. 以正确的顺序添加 `ORDER BY` 列（如果 `ORDER BY` 列有 `DESC` 的话，加上 `DESC`。）；
4. 以任意顺序将 `SELECT` 语句中其余的列添加至索引中（但是需要以不易变的列开始）。

##### 3.7.4.2. 候选B — 避免排序

1. 取出对于优化器来说不过分复杂的等值谓词列，作为索引的前导列 — 以任意顺序皆可。
2. 以正确的顺序添加 `ORDER BY` 列（如果 `ORDER BY` 列有 `DESC` 的话，加上 `DESC`。）；
3. 以任意顺序将 `SELECT` 语句中其余的列添加至索引中（但是需要以不易变的列开始）。

**如果结果集很大的话，为了产生第一页的数据，二星索引后续A（需要排序）可能会花费非常长的时间。**

### 3.8. 设计出色索引的九个步骤

1. 当表结构第 1 版设计（主键、外键、表行顺序）完成时，就开始创建第 0 版的索引：主键索引、外键索引及候选键索引（如果有的话）。
2. 对第 1 版表结构设计的性能表现进行检查：使用 QUBE 评估一些负载事务和批处理程序在理想索引下的响应时间。若评估结果无法满足要求，则将那些具有 1:1 或者 1:C (1对0或1)关系的表进行合并，同时将冗余数据添加至有 1:M (一对多)关系的依赖表中。
3. 当表结构基本稳定后，你可以开始添加一些明显需要的索引 — 基于对应用系统的理解。
4. 若一个表的变化频率很高（如每秒有大于 50 次的插入、更新或删除），那么你应该使用 QUBE 评估一下该表最多容纳有多少个索引。
5. 当知道一个程序的数据库处理模式（事务型或批处理型）后，就需要用最新的数据库版本进行最坏输入下的 QUBE 计算。
   1. 若评估出一个事务的本地响应时间超过了应用的警戒值（如2s），则表明当前的数据库版本无法满足该程序。
   2. 对一个批处理而言，对响应延时的接受度必须针对具体情况逐个评估。如果超过告警阈值，则需要处理：
      1. 对索引改进（半宽索引、宽索引或理想索引）；
      2. 考虑所有情况，对慢查询进行更精确的评估，进而修改表的设计；
      3. 最差情况下，必须与用户协商调整需求，或者与管理人员协商调整硬件配置
6. SQL 语句被编写后，开发人员就应该使用基本问题（BQ），或者如果可行的话，用基础连接问题（BJQ）对其进行评估。
7. 当应用程序发布至生产环境后，有必要进行一次快速的 `EXPLAIN` 检查：对所有引起全表扫描或全索引扫描的 SQL 调用进行分析。这一检查过程也许能发现不合适的索引或优化器问题。
8. 当生产系统正式投入使用后，需要针对首个高峰时段生成一个 LRT 级别的异常报告（尖刺报告或类似的报告）。若一个响应时间问题并非由排队或优化器问题引起，那么你应该用第 5 步中的方法进行处理。
9. 至少每周生成一个 LRT 级别的异常报告。

### 3.9. 索引案例学习

第一件需要考虑的事情是需要使用索引来排序，还是先检索数据再排序。使用索引排序会严格限制索引和查询的设计。

#### 3.9.1. 支持多种过滤条件

需要看看哪些列拥有很多不同的取值，哪些列在 `WHERE` 子句中出现得最频繁。有更多不同值的列上创建索引的选择性会更好。

### 3.10. 维护索引和表

维护表有三个主要目的：

1. 找到并修复损坏的表。
2. 维护准确的索引统计信息。
3. 减少碎片。

#### 3.10.1. 找到并修复损坏的表

损坏的索引导会导致查询返回错误的结果或者莫须有的主键冲突等问题，严重时甚至还会导致数据库的崩溃。

`CHECK TABLE` 通常能够找出大多数表和索引的错误。

`REPAIR TABLE` 来修复损坏的表。

如果存储引擎不支持，也可以通过一个不做任何操作的 `ALTER` 操作来重建表。

如果 InnoDB 引擎的表出现了损坏，那一定是发生了严重的错误，需要立刻调查一下原因。

如果遇到数据损坏，最重要的是找出是什么导致了损坏，而不只是简单地修复，否则很有可能还会不断损坏。

#### 3.10.2. 更新索引统计信息

- `records_in_range()` 通过向存储引擎传入两个边界值获取在这个范围大概有多少记录。
- `info()` 返回各种类型的数据，包括索引的基数（每个键值有多少条记录）。

MySQL 优化器使用的是基于成功的模型，而衡量成本的主要指标就是一个查询需要扫描多少行。如果信息不准确，优化器可能做出错误的决定。

`ANALYZE TABLE` 来重新生成统计信息。

`SHOW INDEX FROM` 来查看索引的基数（Cardinality）。

InnoDB 的统计信息值得深入研究。 InnoDB 引擎通过抽样的方式来计算统计信息，首先随机地读取少量的索引页面，然后以此为样本计算索引的统计信息。

InnoDB 会在表首次打开，或者执行 `ANALYZE TABLE`，抑或表的大小发生非常大的变化时计算索引的统计信息。

#### 3.10.3. 减少索引和数据的碎片

B-Tree 索引可能会碎片化，这会降低查询的效率。碎片化的索引可能会以很差或者无序的方式存储在磁盘上。

根据设计，B-Tree 需要随机磁盘访问才能定位到叶子页，所以随机访问是不可避免的。然而，如果叶子页在物理分布上是顺序且紧密的，那么查询的性能就会更好。否则，对于范围查询、索引覆盖扫描等操作来说，速度可能会降低很多倍；对于索引覆盖扫描这一点更加明显。

如果叶子页在物理分布上是顺序且紧密的，那么查询的性能就会更好。

数据存储的碎片化有三种类型：

- **行碎片（Row fragementation）**

  指的是数据行被存储为多个地方的多个片段中。即使查询只从索引中访问一行记录，行碎片也会导致性能下降。

- **行间碎片（Intra-row fragementation）**

  指逻辑上顺序的页，或者行在磁盘上不是顺序存储的。对全表扫描或聚簇索引扫描之类的操作有很大的影响。

- **剩余空间碎片（Free space fragementation）**

  指数据页中有大量的空余空间。会导致服务器读取大量不需要的数据，从而造成浪费。

对于 MyISAM 表，这三类碎片化都可能发生。但 InnoDB 不会出现短小的行碎片；InnoDB 会移动短小的行并重写到一个片段中。

`OPTIMIZE TABLE` 或者导出再导入的方式重新整理数据。

对不支持 `OPTIMIZE TABLE` 的存储引擎，可以通过一个不做任何操作的 `ALTER TABLE` 操作来重建表。只需要将表的存储引擎修改为当前的引擎即可：

```mysql
1 ALTER TABLE <table> ENGINE=<engine>;
```

### 3.11. 总结

在选择索引和编写利用这些索引的查询时，有三个原则始终需要记住：

1. 单行访问时很慢的。最好读取的块中能包含尽可能多所需要的行。
2. 按顺序访问范围数据是很快的。
   1. 顺序 I/O 不需要多次磁盘寻道，所以比随机 I/O 要快很多
   2. 如果服务器能够按需要顺序读取数据，那么久不再需要额外的排序操作，并且 `GROUP BY` 查询也无须再做排序和将行按组进行聚合计算了。
3. 索引覆盖查询很快。

这与上完提到的 [“三星索引”](https://notes.diguage.com/mysql/#three-star-system) 是一致的。

## 4. 查询性能优化

查询优化、索引优化、库表结构优化需要齐头并进，一个不落。

### 4.1. 为什么查询速度会慢？

真正重要的是响应时间。

查询的生命周期大致可以按照顺序来看：从客户端，到服务器，然后在服务器上进行解析，生成执行计划，执行，并返回结果给客户端。在完成这些任务的时候，查询需要在不同的地方花费时间，包括网络，CPU 计算，生成统计信息和执行计划、锁等待（互斥等待）等操作，尤其是向地城存储引擎检索数据的调用，这些调用需要在内存操作、CPU 操作和内存不足时导致的 I/O 操作上消耗时间。

了解查询的生命周期、清楚查询的时间消耗情况对于优化查询有很大的意义。

### 4.2. 慢查询基础：优化数据访问

查询性能低下最基本的原因是访问的数据太多。

对于低效的查询，下面两步分析总是有效的：

1. 确认应用程序是否在检索大量超过需要的数据。访问了太多的行，有时也可能访问太多的列。
2. 确认 MySQL 服务器层是否在分析大量超过需要的数据行。

查询大量不需要的数据的典型案例：

1. 查询不需要的记录
   - 最简单有效的解决方法就是在这样的查询后面加上 `LIMIT`。
2. 多表关联时返回全部列
3. 总是取出全部列
   - 每次看到 `SELECT *` 时都需要用怀疑的眼光审视，是不是真的需要返回全部的列！
4. 重复查询相同的数据

最简单的衡量查询开销的三个指标如下：

- 响应时间
- 扫描的行数
- 返回的行数

这三个指标都会记录到 MySQL 的慢日志中，所以检查慢日志记录是找出扫描行数过多的查询的好办法。

代码 14. 查看 MySQL 慢日志配置

```mysql
1 SHOW GLOBAL VARIABLES LIKE '%slow%';
```

|      | 可以写一个脚本来分析 MySQL 的日志，进而找出比较慢的查询。 |
| ---- | --------------------------------------------------------- |

- 响应时间

  响应时间只是表面上的一个值；但，响应时间仍然是最重要的指标。响应时间是两部分之和：服务时间和排队时间。服务时间指数据库处理这个查询真正花了多长时间。排队时间指服务器因为等待某些资源而没有真正执行查询的时间—可能是等 I/O 操作完成，也可能是等待行锁等待。一般最常见和重要的等待是 I/O 和锁等待。

[数据库索引设计与优化](https://book.douban.com/subject/26419771/) 一书讲述了一种估算查询的响应时间方法：快速上限估计。概括地说，了解这个查询需要哪些索引以及它的执行计划是什么，然后计算大概需要多少个顺序和随机 I/O，再用其乘以在具体硬件条件下一次 I/O 的消耗时间。最后把这些消耗都加起来，就可以获得一个大概参考值来判断当前响应时间是不是一个合理的值。

> **快速上限评估算法（QUBE）**
>
> - 比较值
>
>   LRT = TR * 10mx + TS * 0.01ms
>
> - 绝对值
>
>   LRT = TR * 10mx + TS * 0.01ms + F * 0.1ms
>
>   LRT = 本地相应时间
>
>   TR = 随机访问的数量
>
>   TS = 顺序访问的数量
>
>   F = 有效 Fetch 的数量

— Tapio Lahdenmaki & Michael Leach数据库索引设计与优化

- 扫描的行数和返回的行数

  并不是所有的行的访问代价是相同的。较短的行的访问速度更快，内存中的行也比磁盘中的行的访问速度要快得多。理想情况下扫描的行数和返回的行数应该是相同的。扫描的行数对返回的行数比率通常很小，一般在 1:1 和 10:1 之间。

- 扫描的行数和访问类型

  在评估查询开销的时候，需要考虑一下从表中找到某一行数据的成本。在 `EXPLAIN` 语句中的 `type` 列反应了访问类型。访问类型有很多种，从全表扫描到索引扫描、范围扫描、唯一索引查询、常数引用等。

如果查询没有办法找到合适的访问类型，那么解决的最好办法通常就是增加一个合适的索引。索引让 MySQL 以最高效、扫描行数最少的方式找到需要的记录。

```mysql
USE sakila;

EXPLAIN
SELECT *
FROM film_actor
WHERE film_id = 1;
# 从下面的结果也能看出，MySQL 在索引 idx_fk_film_id 上使用了 `ref` 访问类型来执行 SQL。
mysql> EXPLAIN SELECT * FROM film_actor WHERE film_id = 1 \G 
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: film_actor
   partitions: NULL
         type: ref
possible_keys: idx_fk_film_id
          key: idx_fk_film_id
      key_len: 2
          ref: const
         rows: 10
     filtered: 100.00
        Extra: NULL
1 row in set, 1 warning (0.00 sec)

ALTER TABLE film_actor
  DROP FOREIGN KEY fk_film_actor_film;

ALTER TABLE film_actor
  DROP KEY idx_fk_film_id;

EXPLAIN
SELECT *
FROM film_actor
WHERE film_id = 1;

# 删除索引后，访问类型变成了一个全表扫描（ `ALL` ），现在 MySQL 预估需要扫描 5073 条记录来完成这个查询。 `Using where` 表示 MySQL 将通过 `WHERE` 条件来筛选存储引擎返回的记录。
mysql> EXPLAIN SELECT * FROM film_actor WHERE film_id = 1 \G 
*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: film_actor
   partitions: NULL
         type: ALL
possible_keys: NULL
          key: NULL
      key_len: NULL
          ref: NULL
         rows: 5462
     filtered: 10.00
        Extra: Using where
1 row in set, 1 warning (0.00 sec)
```

一般 MySQL 能够使用如下三种方式应用 `WHERE` 条件，从好到坏以此为：

- 在索引中使用 `WHERE` 条件来过滤不匹配的记录。这是在存储引擎层完成的。
- 使用索引覆盖扫描（在 `Extra` 列中出现了 `Using index`）来返回记录，直接从索引中过滤掉不需要的记录并返回命中的结果。这是在 MySQL 服务器层完成的，但无须再回表查询记录。
- 从数据表中返回数据，然后过滤掉不满足条件的记录（在 `Extra` 列中出现 `Using Where`）。这在 MySQL 服务器层完成，MySQL 需要先从数据表读出记录然后过滤。

好的索引可以让查询使用合适的访问类型，尽可能地值扫描需要的数据行。但也不是说增加索引就能让扫描的行数等于返回的行数。例如 `COUNT(*)` 查询。

不幸的是，MySQL 不会告诉我们生成结果实际上需要扫描多少行数据，而只会告诉我们生成结果时一共扫描了多少行数据。扫描的行数中的大部分都很可能是被 `WHERE` 条件过滤掉的，对最终的结果集并没有贡献。理解一个查询需要扫描多少行和实际需要使用的行数需要先去理解这个查询背后的逻辑和思想。

如果发现查询需要扫描大量的数据但只返回少数的行，那么通常可以尝试下面的技巧去优化它：

- 使用索引覆盖扫描，把所有需要用到的列都放到索引中，这样存储引擎无须回表获取对应行就可以返回结果了。
- 改变库表结构。例如使用单独的汇总表。
- 重写这个复杂的查询，让 MySQL 优化器能够以更优化的方式执行这个查询。

### 4.3. 重构查询的方式

在优化有问题的查询时，目标应该是找到一个更优的方法获取实际需要的结果—而不一定总是需要从 MySQL 获取一模一样的结果集。

#### 4.3.1. 一个复杂查询还是多个简单查询

设计查询的时候一个需要考虑的重要问题是，是否需要将一个复杂的查询分成多个简单的查询。

MySQL 从设计上让连接和断开连接都 很轻量级，在返回一个小的查询结果方面很高效。

MySQL 内部每秒能够扫描内存中上百万行数据。

在应用设计的时候，如果一个查询能够胜任时还写成多个独立查询是不明智的。

#### 4.3.2. 切分查询

有时候对于一个大查询我们需要“分而治之”，将大查询切分成小查询，每个查询功能完全一样，只完成一小部分，每次只返回一小部分查询结果。例如删除旧的数据。

|      | 这个原则不仅仅适用于数据库，在很多地方都适用。 |
| ---- | ---------------------------------------------- |

#### 4.3.3. 分解关联查询

可以对每一个表进行一次单表查询，然后将结果在应用程序中进行关联。

用分解关联查询的方式重构查询有如下的优势：

- 让缓存的效率更高。
- 将查询分解后，执行单个查询可以减少锁的竞争。
- 在应用层做关联，可以更容易对数据库进行拆分，更容易做到高性能和可扩展。
- 查询本身效率也可能会有所提升。
- 可以减少冗余记录的查询。
- 这样做相当于在应用中实现了哈希关联，而不是使用 MySQL 的嵌套循环关联。某些场景哈希关联的效率要高很多。

### 4.4. 查询执行的基础

当希望 MySQL 能够以更高的性能运行查询时，最好的办法就是弄清楚 MySQL 是如何优化和执行查询的。

![image-20220123202850693](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123202850693-2d2bf2.png)



图 42. 查询执行路径

当我们向 MySQL 发送一个请求的时候， MySQL 执行如下操作：

1. 客户端发送一条查询给服务器。
2. 服务器先检查查询缓存，如果命中了缓存，则立刻返回存储在缓存中的结果。否则进入下一阶段。
3. 服务器进行 SQL 解析、预处理，再由优化器生成对应的执行计划。
4. MySQL 根据优化器生成的执行计划，调用存储引擎的 API 来执行查询。
5. 将结果返回给客户端。

#### 4.4.1. MySQL 客户端/服务器通信协议

一般来说，不需要去理解 MySQL 通信协议的内部实现细节，只需要大致理解通信协议是如何工作的。MySQL 客户端和服务器之间的通信心意是“半双工”的，这意味着，在任何一个时刻，要么是由服务器向客户端发送数据，要么是由客户端向服务器发送数据，这两个动作不能同时发生。所以，我们无法也无须将一个消息切分成小块独立来发送。

通信简单，也有很多限制。一个明显的限制是，这意味着没法进行流量控制。一旦一段开始发送消息，另一段要接收完整个消息才能响应它。

客户端用一个独立的数据包将查询传给服务器。

相反的，一般服务器响应给用户的数据通常很多，由多个数据包组成。当服务器开始响应客户端请求时，客户端必须完整地接收整个返回结果，而不能简单地只取前面几条结果，然后让服务器停止发送数据。这也是在必要的时候一定要在查询中加上 `LIMIT` 限制的原因。

当客户端从服务器取数据时，看起来是一个拉数据的过程，但实际上是 MySQL 在向客户端推送数据的过程。客户端没法让服务器停下来。

多数连接 MySQL 的库函数都可以获得全部结果集并缓存到内存里，还可以逐行获取需要的数据。默认一般是获得全部结果集并缓存到内存中。

当使用多数连接 MySQL 的库函数从 MySQL 获取数据时，其结果看起来都像是从 MySQL 服务器获取数据，而实际上都是从这个库函数的缓存获取数据。

|      | 这里的意思是，处理 `ResultSet` 时，数据已经从 MySQL 服务器上读取过来数据，然后直接从 `ResultSet` 中取数据。 |
| ---- | ------------------------------------------------------------ |

- 查询状态

  对于一个 MySQL 连接，或者说一个线程，任何时刻都有一个状态，该状态表示了 MySQL 当前正在做什么。有很多方式查看当前的状态，最简单的是使用 `SHOW FULL PROCESSLIST` 命令。Sleep线程正在等待客户端发送新的请求。Query线程正在执行查询或者正在将结果发送给客户端。Locked在 MySQL 服务器层，该线程正在等待表锁。在存储引擎级别实现的锁，例如 InnoDB 的行锁，并不会体现在线程状态中。Analyzing and statistics线程正在收集存储引擎的统计信息，并生成查询的执行计划。Copying to tmp table [on disk]线程正在执行查询，并且将结果集都复制到一个临时表中，这种状态一般要么是在做 `GROUP BY` 操作，要么是文件排序操作，或者是 `UNION` 操作。如果这个状态后面还有 `on disk` 标记，那表示 MySQL 正在将一个内存临时表放到磁盘上。Sorting result线程正在对结果集进行排序。Sending data这表示多种情况：线程可能在多个状态之间传送数据，或者在生成结果集，或者在向客户端返回数据。

#### 4.4.2. 查询缓存

在解析一个查询语句之前，如果查询缓存是打开的，那么 MySQL 会优先检查这个查询是否命中查询缓存中的数据。检查是通过对大小写敏感的哈希查找实现的。不匹配则进行下一阶段处理。

命中缓存，那么在返回结果前 MySQL 会检查一次用户权限。如果没有问题，则直接从缓存中拿到结果返回给客户端。这种情况下，查询不会被解析，不用生成执行计划，不会执行。

#### 4.4.3. 查询优化处理

查询的生命周期的下一步是将一个 SQL 转换成一个执行计划，MySQL 再按照这个执行计划和存储引擎进行交互。这包含多个子阶段： 解析 SQL、预处理、优化 SQL 执行计划。

##### 4.4.3.1. 语法解析器和预处理

首先，MySQL 通过关键字将 SQL 语句进行解析，并生成一课对应的“解析树”。MySQL 解析器将使用 MySQL 语法规则验证和解析查询。

预处理器则根据一些 MySQL 规则进一步检查解析树是否合法。

下一步预处理器会验证权限。通常很快，除非有非常多的权限配置。

##### 4.4.3.2. 查询优化器

一条查询可以有很多种执行方式，最后都返回相同的结果。优化器的作用就是找到这其中最好的执行计划。

MySQL 使用基于成本的优化器，它将尝试预测一个查询使用某种执行计划时的成本，并选择其中成本最小的一个。可以通过查询当前会话的 `Last_query_cost` 的值来得知 MySQL 计算的当前查询的成本。

```mysql
USE sakila;

SELECT SQL_NO_CACHE count(*)
FROM film_actor;
# 在不同机器上，结果可能不一样。
SHOW STATUS LIKE 'Last_query_cost'; 

```

这是根据一系列的统计信息计算得来的：每个表或者索引的页面个数、索引的基数（索引中不同值的数量）、索引和数据行的长度、索引分布情况。

优化器在评估成本的时候并不考虑任何层面的缓存，它假设读取任何数据都需要一次磁盘 I/O。

导致 MySQL 优化器选择错误的执行计划的原因：

- 统计信息不准确。 MySQL 依赖存储引擎提供的统计信息来评估成本，但是有的存储引擎提供的信息是准确的，有的偏差可能非常大。
- 执行计划中的成本估算不等同于实际执行的成本。所以即使统计信息精确，优化器给出的执行计划也可能不是最优的。
- MySQL 的最优可能和你想的最优不一样。由此可见，根据执行成本选择执行计划并不是完美的模型。
- MySQL 从不考虑其他并发执行的查询，这可能会影响到当前查询的速度。
- MySQL 也并不是任何时候都是基于成本的优化。例如全文检索。
- MySQL 不会考虑不受其控制的操作的成本。
- 优化器有时无法去估算所有可能的执行计划。

MySQL 的查询优化器是一个非常复杂的部件，它使用了很多优化策略来生成一个最优的执行计划。优化策略可以简单地分为两种，一种是静态优化，一种是动态优化。静态优化可以直接对解析树进行分析，并完成优化。静态优化不依赖于特别的数值。静态优化在第一次完成后就一直有效，即使使用不同的参数值重复执行查询也不会发生变化。可以认为这是一种“编译时优化”。

动态优化则和查询的上下文有关，也可能和很多其他因素有关，需要在每次查询时都重新评估，可以认为是“运行时优化”。有时甚至在查询的执行过程中也会重新优化。

MySQL 能够处理的优化类型：

- 重新定义关联表的顺序

  数据表的关联并不总是安装在查询中指定的顺序进行。决定关联的顺序是优化器很重要的一部分功能。

- 将外连接转化成内连接

  并不是所有的 `OUTER JOIN` 语句都必须以外连接的方式执行。

- 使用等价变换规则

  MySQL 可以使用一些等价变换来简化并规范表达式。可以科比能够一些比较，移除一些恒成立和一些恒不成立的判断等等。

- 优化 `COUNT()`、`MIN()` 和 `MAX()`

  索引和列是否可为空通常可以帮助 MySQL 优化这类表达式。例如：从 B-Tree 索引中取最大值或者最小值；没有任何 `WHERE` 条件的 `COUNT(*)` 查询。

- 预估并转化为常数表达式

  当 MySQL 检测到一个表达式可以转化为常数的时候，就会一直把该表达式作为常数进行优化处理。让人惊讶的是，在优化阶段，有时候甚至一个查询也能够转化为一个常数。例如：在索引列上执行 `MIN()`；甚至主键或者唯一键查找语句。

  ```mysql
  USE sakila;
  
  EXPLAIN
  SELECT
    f.film_id,
    fa.actor_id
  FROM film f
    INNER JOIN film_actor fa USING (film_id)
  WHERE f.film_id = 1 \G
  
  *************************** 1. row ***************************
             id: 1
    select_type: SIMPLE
          table: f
     partitions: NULL
           type: const
  possible_keys: PRIMARY
            key: PRIMARY
        key_len: 2
            ref: const
           rows: 1
       filtered: 100.00
          Extra: Using index
  *************************** 2. row ***************************
             id: 1
    select_type: SIMPLE
          table: fa
     partitions: NULL
           type: ref
  possible_keys: idx_fk_film_id
            key: idx_fk_film_id
        key_len: 2
            ref: const
           rows: 10
       filtered: 100.00
          Extra: Using index
  ```

  

  MySQL 分两步来执行查询。

   `第一步从 `film` 表找到需要的行。因为在 `film_id` 字段上有主键索引，所以 MySQL 优化器知道这只会返回一行数据，优化器在生成执行计划的时候，就已经通过索引信息知道将返回多少行数据。因为优化器已经明确知道有多少个值（ `WHERE` 条件中的值）需要做索引查询，所以这里的表访问类型是 `const`。 第二步，MySQL 将第一步中返回的 `film_id` 列当做一个已知取值的列来处理。因为优化器清楚再第一步执行完成后，该值就会是明确的了。注意到正如第一步中一样，使用 `film_actor` 字段对表的访问类型也是 `const`。P212另一种会看到常数条件的情况是通过等式将常数值从一个表传给另一个表，这可以通过 `WHERE`、`USING` 或者 `ON` 语句来限制某列值为常数。

- 覆盖索引扫描

  当索引中的列包含所有查询中需要使用的列的时候， MySQL 就可以使用索引返回需要的数据，而无须查询对应的数据行。

- 子查询优化

  MySQL 在某些情况下可以将子查询转换成一种效率更高的形式，从而减少多个查询多次对数据进行访问。

- 提前终止查询

  在发现已经满足查询需求的时候，MySQL 总是能够立刻终止查询。例如：`LIMIT` 子句；再例如，发现一个不成立的条件。

  ```mysql
  USE sakila;
  
  EXPLAIN
  SELECT film_id
  FROM film
  WHERE film_id = -1 \G
  *************************** 1. row ***************************
             id: 1
    select_type: SIMPLE
          table: NULL
     partitions: NULL
           type: NULL
  possible_keys: NULL
            key: NULL
        key_len: NULL
            ref: NULL
           rows: NULL
       filtered: NULL
          Extra: no matching row in const table
  ```

  

  从这个例子看到，查询在优化阶段就已经终止。

- 等值传播

  如果两个列的值通过等式关联，那么 MySQL 能够把其中一个列的 `WHERE` 条件传递到另一列上。

- 列表 `IN()` 的比较

  在很多数据库系统中，`IN()` 完全等同于多个 `OR` 条件的子句，因为这两者是完全等价的。而 MySQL 将 `IN()` 列表中的数据先进行排序，然后通过二分查找的方式来确定列表中的值是否满足条件，这是 O(log *n*) 复杂度；转化成 `OR` 查询则为 O(*n*)。

**不要自以为比优化器更聪明！**

##### 4.4.3.3. 数据和索引的统计信息

不同的存储引擎可能会存储不同的统计信息（也可以按照不同的格式存储统计信息）。

MySQL 查询优化器在生成查询的执行计划时，需要向存储引擎获取相应的统计信息。存储引擎则提供给优化器对应的统计信息，包括：每个表或者索引有多少个页面、每个表的每个索引的基数是多少、数据行和索引长度、索引的分布信息等等

##### 4.4.3.4. MySQL 如何执行关联查询

MySQL 认为任何一个查询都是一次“关联” — 并不仅仅是一个查询需要到两个表匹配才叫关联，所以在 MySQL 中，每一个查询，每一个片段（包括子查询，甚至基于单表的 `SELECT`）都可能使关联。

对于 `UNION` 查询，MySQL 先将一系列的单个查询结果放到一个临时表中，然后再重新读出临时表数据来完成 `UNION` 查询。

MySQL 关联执行的策略：MySQL 对任何关联都执行嵌套循环关联操作，即 MySQL 先在一个表中循环取出单条数据，然后再嵌套循环到下一个表中寻找匹配的行，依次下去，知道找到所有表中匹配的行位置。然后根据各个表匹配的行，返回查询中需要的各个列。MySQL 会尝试在最后一个关联表中找到所有匹配的行，如果最后一个关联表无法找到更多的行以后，MySQL 返回到上一层次关联表，看是否能够找到更多的匹配记录，以此类推迭代执行。可以使用如下代码来解释：

```mysql
-- 内关联查询 ----------------------------------------------------
SELECT
  tbl1.col1,
  tbl2.col2
FROM tbl1
  INNER JOIN tbl2 USING (col3)
WHERE tbl1.col1 IN (5, 6);

-- 用伪代码来解释 MySQL 关联执行的策略则是如下：
outer_iter = iteratro over tbl1 WHERE col1 IN (5, 6)
outer_row = outer_iter.next
while outer_row
    inner_iter = iteratro over tbl2 WHERE col3 = outer_row.col3
    inner_row  = inner_iter.next
    while inner_row
        output [outer_row.col1, inner_row.col2]
        inner_row = inner_iter.next
    end
    outer_row = outer_iter.next
end

-- 左外关联查询 --------------------------------------------------

SELECT
  tbl1.col1,
  tbl2.col2
FROM tbl1
  LEFT OUTER JOIN tbl2 USING (col3)
WHERE tbl1.col1 IN (5, 6);

-- 用伪代码来解释 MySQL 关联执行的策略则是如下：
outer_iter = iteratro over tbl1 WHERE col1 IN (5, 6)
outer_row = outer_iter.next
while outer_row
    inner_iter = iteratro over tbl2 WHERE col3 = outer_row.col3
    inner_row  = inner_iter.next
    if inner_row
        while inner_row
            output [outer_row.col1, inner_row.col2]
            inner_row = inner_iter.next
        end
    else
        output [outer_row.col1, NULL]
    end
    outer_row = outer_iter.next
end
```

可视化查询执行计划的方法是根据优化器执行的路径绘制出对应的“泳道图”。

![image-20220123203139071](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123203139071-adb957.png)

图 43. 关联查询泳道图

从本质上来说，MySQL 对所有的类型的查询都以同样的方式运行。例如：子查询先放到一个临时表；`UNION` 也用类似的临时表。

|      | 在 MySQL 5.6 和 MariaDB 中有了重大改变，这两个版本都引入了更加复杂的执行计划。 |
| ---- | ------------------------------------------------------------ |

##### 4.4.3.5. 执行计划

MySQL 生成查询的一棵指令树，然后通过存储引擎执行完成这颗指令树并返回结果。最终的执行计划包含了重构查询的全部信息。

如果读某个查询执行 `EXPLAIN EXTENDED` 后，再执行 `SHOW WARNINGS`，就可以看到重构出的查询。

##### 4.4.3.6. 关联查询优化器

MySQL 优化器最重要的一部分就是关联查询优化，它决定了多个表关联时的顺序。关联查询优化器通过评估不同关联顺序时的成本来选择一个代价最小的关联顺序。

```mysql
USE sakila;

EXPLAIN
SELECT
  film.film_id,
  film.title,
  film.release_year,
  actor.actor_id,
  actor.first_name,
  actor.last_name
FROM film
  INNER JOIN film_actor USING (film_id)
  INNER JOIN actor USING (actor_id) \G

*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: actor
   partitions: NULL
         type: ALL
possible_keys: PRIMARY
          key: NULL
      key_len: NULL
          ref: NULL
         rows: 200
     filtered: 100.00
        Extra: NULL
*************************** 2. row ***************************
           id: 1
  select_type: SIMPLE
        table: film_actor
   partitions: NULL
         type: ref
possible_keys: PRIMARY,idx_fk_film_id
          key: PRIMARY
      key_len: 2
          ref: sakila.actor.actor_id
         rows: 27
     filtered: 100.00
        Extra: Using index
*************************** 3. row ***************************
           id: 1
  select_type: SIMPLE
        table: film
   partitions: NULL
         type: eq_ref
possible_keys: PRIMARY
          key: PRIMARY
      key_len: 2
          ref: sakila.film_actor.film_id
         rows: 1
     filtered: 100.00
        Extra: NULL
3 rows in set, 1 warning (0.00 sec)
```

从这个执行计划就能能看出这个查询是从 `actor` 开始查询的。对比一下：

```mysql
USE sakila;

EXPLAIN
SELECT STRAIGHT_JOIN
  film.film_id,
  film.title,
  film.release_year,
  actor.actor_id,
  actor.first_name,
  actor.last_name
FROM film
  INNER JOIN film_actor USING (film_id)
  INNER JOIN actor USING (actor_id) \G

*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: film
   partitions: NULL
         type: ALL
possible_keys: PRIMARY
          key: NULL
      key_len: NULL
          ref: NULL
         rows: 1000
     filtered: 100.00
        Extra: NULL
*************************** 2. row ***************************
           id: 1
  select_type: SIMPLE
        table: film_actor
   partitions: NULL
         type: ref
possible_keys: PRIMARY,idx_fk_film_id
          key: idx_fk_film_id
      key_len: 2
          ref: sakila.film.film_id
         rows: 5
     filtered: 100.00
        Extra: Using index
*************************** 3. row ***************************
           id: 1
  select_type: SIMPLE
        table: actor
   partitions: NULL
         type: eq_ref
possible_keys: PRIMARY
          key: PRIMARY
      key_len: 2
          ref: sakila.film_actor.actor_id
         rows: 1
     filtered: 100.00
        Extra: NULL
```

如果优化器给出的并不是最优的关联顺序，可以使用 `STRAIGHT_JOIN` 关键字重新查询，让优化器按照你认为的最优的关联顺序执行。绝大多数时候，优化器做出的选择都比普通人的判断更准确。

关联优化器会尝试在所有的关联顺序中选择一个成本最小的来生成执行计划树。

糟糕的是，如果有超过 n 个表关联，那么需要检查 n 的阶乘种关联关系，称之为所有可能的执行计划的 “搜索空间”，搜索空间的增长非常快。当搜索空间非常大的时候，优化器不可能逐一评估每一种关联顺序的成本，优化器选择使用“贪婪”搜索的方式查找“最优”的关联顺序。

##### 4.4.3.7. 排序优化

无论如何排序都是一个成本很高的操作，所以从性能角度考虑，应尽可能避免排序或者尽可能避免对大量数据进行排序。

如果需要排序的数量小于“排序缓冲区”，MySQL 使用内存进行“快速排序”操作。如果内存不够排序，那么 MySQL 会先将数据分块，对每个独立的块使用“快速排序”进行排序，并将各个块的排序结果存放在磁盘上，然后将各个排好序的块进行合并，最后返回排序结果。

MySQL 有如下两种排序算法：

- 两次传输排序（旧版本使用）

  读取行指针和需要排序的字段，对其进行排序，然后再根据排序结果读取所需要的数据行。需要两次数据传输，即需要从数据表中读取两次数据，第二次读取数据的时候，因为是读取排序列进行排序后的所有记录，会产生大量的随机 I/O。优点：在排序的时候存储尽可能少的数据，让“排序缓冲区”中可能容纳尽可能多的行数进行排序。

- 单次传输排序（新版本使用）

  先读取查询所需要的所有列，然后再根据给定列进行排序，最后直接返回排序结果。在 MySQL 4.1 和后续更新的版本才引入。优点：不需要读取两次数据，对于 I/O 密集型的应用，效率高很多，只需一次顺序 I/O 读取所有的数据，无须任何的随机 I/O。缺点：如果返回的列非常多、非常大，会额外占用大量的空间。

|      | 可以通过调整 `max_length_for_sort_data` 来影响 MySQL 排序算法的选择。 |
| ---- | ------------------------------------------------------------ |

|      | MySQL 在进行文件排序的时候需要使用的临时存储空间可能会比想象的要大得多。 |
| ---- | ------------------------------------------------------------ |

如果 `ORDER BY` 子句中的所有列都来自关联的第一个表，那么 MySQL 在关联处理第一个表的时候就进行文件排序。如果是这样，那么在 MySQL 的 `EXPLAIN` 结果中可以看到 `Extra` 字段会有 `Using filesort`。除此之外的所有情况，MySQL 都会先将管理的结果存放到一个临时表中，然后在所有的关联都结束后，再进行文件排序。这时，在 MySQL 的 `EXPLAIN` 结果的 `Extra` 字段可以看到 `Using temporary; Using filesort`。`LIMIT` 会在排序后应用。

MySQL 5.6 当还需要返回部分查询结果时，不再对所有结果进行排序。

|      | 从这句话中也可以看出，如果可以，尽量使用一张表中的字段。 |
| ---- | -------------------------------------------------------- |

#### 4.4.4. 查询执行引擎

查询执行阶段不是那么复杂：MySQL 只是简单地根据执行计划给出的指令逐步执行。

存储引擎接口有着非常丰富的功能，但底层接口却只有几十个，这些接口像“搭积木”一样能够完成查询的大部分操作。

#### 4.4.5. 返回结果给客户端

查询执行的最后一个阶段是将结果返回给客户端。

如果查询可以被缓存，那么 MySQL 在这个阶段也会将结果存放到查询缓存中。

MySQL 将结果集返回客户端是一个增量、逐步返回的过程。

### 4.5. MySQL 查询优化器的局限性

MySQL 的万能“嵌套循环”并不是对每种查询都是最优的。MySQL 查询优化器只对少部分查询不适用，往往可以通过改写查询让 MySQL 高效地完成工作。

#### 4.5.1. 关联子查询

MySQL 的子查询实现得非常糟糕。最糟糕的一类查询是 `WHERE` 条件中包含 `IN()` 的子查询语句。

```mysql
USE sakila;

-- 原始写法
SELECT *
FROM film
WHERE film_id IN (
  SELECT film_id
  FROM film_actor
  WHERE actor_id = 1);

-- 改进后的写法
SELECT film.*
FROM film
  INNER JOIN film_actor USING (film_id)
WHERE actor_id = 1;

-- 书上提到的第二种写法，但是书上前后矛盾，
-- 查看执行计划也发现，这种写法有问题。
SELECT *
FROM film
WHERE EXISTS(
    SELECT *
    FROM film_actor
    WHERE actor_id = 1
          AND film_actor.film_id = film.film_id);

```

|      | 在 MySQL 5.7 中，上面第一种 SQL 存在的问题已经得到解决。可以和第二种有同样的表现。 |
| ---- | ------------------------------------------------------------ |

##### 4.5.1.1. 如何用好关联子查询

并不是所有关联子查询的性能都会很差。先测试，然后做出自己的判断。很多时候，关联子查询是一种非常合理、自然，甚至是性能最好的写法。

```mysql
USE sakila;

EXPLAIN
SELECT
  film_id,
  language_id
FROM film
WHERE NOT EXISTS(
    SELECT *
    FROM film_actor
    WHERE film_actor.film_id = film.film_id);

*************************** 1. row ***************************
           id: 1
  select_type: PRIMARY
        table: film
   partitions: NULL
         type: index
possible_keys: NULL
          key: idx_fk_language_id
      key_len: 1
          ref: NULL
         rows: 1000
     filtered: 100.00
        Extra: Using where; Using index 
        # 2
*************************** 2. row ***************************
           id: 2
  select_type: DEPENDENT SUBQUERY  	
  				# 1
        table: film_actor
   partitions: NULL
         type: ref
possible_keys: idx_fk_film_id
          key: idx_fk_film_id
      key_len: 2
          ref: sakila.film.film_id
         rows: 5
     filtered: 100.00
        Extra: Using index  
			# 3

-- 使用左外链接“优化”后的 SQL
EXPLAIN
SELECT
  film.film_id,
  film.language_id
FROM film
  LEFT OUTER JOIN film_actor USING (film_id)
WHERE film_actor.film_id IS NULL \G

*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: film
   partitions: NULL
         type: index
possible_keys: NULL
          key: idx_fk_language_id
      key_len: 1
          ref: NULL
         rows: 1000
     filtered: 100.00
        Extra: Using index 
        # 2
*************************** 2. row ***************************
           id: 1
  select_type: SIMPLE  
  # 1
        table: film_actor
   partitions: NULL
         type: ref
possible_keys: idx_fk_film_id
          key: idx_fk_film_id
      key_len: 2
          ref: sakila.film.film_id
         rows: 5
     filtered: 100.00
        Extra: Using where; Not exists; Using index  
```

| 1    | 表 film_actor 的访问类型一个是 `DEPENDENT SUBQUERY`，另外一个是 `SIMPLE`。这是由于语句的写法不同导致的，一个是普通查询，一个是子查询。对于底层存储引擎接口来说，没有任何不同。 |
| ---- | ------------------------------------------------------------ |
| 2    | 对于 film 表，第二个查询的 `Extra` 中没有 `Using where`，但不重要，第二个查询的 `USING` 子句和第一个查询的 `WHERE` 子句实际上是完全一样的。 |
| 3    | 第二个表 film_actor 的执行计划的 `Extra` 列有 `Not exists`。这是提前终止算法（early-termination algorithm），MySQL 通过使用 `Not exists` 优化来避免在表 film_actor 的索引中读取额外的行。这完全等效于直接编写 `NOT EXISTS` 子查询。 |

综上，从理论上来讲，MySQL 将使用完全相同的执行计划来完成这个查询。

**再次强调：应该用测试来验证对子查询的执行计划和响应时间的假设！**

#### 4.5.2. `UNION` 的限制

MySQL 无法将限制条件从外层“下推”到内层。例如，无法将 `LIMIT` “下推”到 `UNION` 的各个子句。

#### 4.5.3. 索引合并优化

在 MySQL 5.0 和更新的版本中，当 `WHERE` 子句中包含多个复杂条件的时候，MySQL 能够访问单个表的多个索引以合并和交叉过滤的方式来定位需要查找的行。

#### 4.5.4. 等值传递

某些时候，等值传递会带来一些意想不到的额外消耗。例如，一个非常大的 `IN()` 列表。

#### 4.5.5. 并行执行

MySQL 无法利用多核特性来并行执行查询。

#### 4.5.6. 哈希关联

MariaDB 已经实现了真正的哈希关联。

#### 4.5.7. 松散索引扫描

MySQL 并不支持松散索引扫描。通常，MySQL 的索引扫描需要先定义一个起点和终点，即使需要的数据只是这段索引中很少数的几个，MySQL 仍需要扫描这段索引中每一个条目。

例如：所以字段是（a, b），查询 b 字段区间值。可以逐个 a 去定位指点 b，这样效果就会很好。

MySQL 5.0 之后的版本，在某些特殊的场景下是可以使用松散索引扫描的，例如，在一个分组查询中需要找到分组的最大值和最小值：

```mysql
EXPLAIN
SELECT
  actor_id,
  max(film_id)
FROM film_actor
GROUP BY actor_id \G

*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: film_actor
   partitions: NULL
         type: range
possible_keys: PRIMARY,idx_fk_film_id
          key: PRIMARY
      key_len: 2
          ref: NULL
         rows: 201
     filtered: 100.00
        Extra: Using index for group-by
```

在 `EXPLAIN` 的 `Extra` 字段显示 “Using index for group-by”，表示这里将使用松散索引扫描。如果 MySQL 能写上 “loose index probe”，相信会更好理解。

一个简单的绕过问题的办法就是给前面的列加上可能的常数值。

在 MySQL 5.6 之后的版本，关于松散索引扫描的一些限制会通过“索引条件下推（index condition pushdown）”的方式来解决。

#### 4.5.8. 最大值和最小值优化

对于 `MIN()` 和 `MAX()` 查询，MySQL 的优化做得并不好。

```mysql
SELECT MIN(actor_id)
FROM actor
WHERE first_name = 'PENELOPE';

```

`first_name` 字段没有索引，会做一次全表扫描。如能使用主键扫描，当 MySQL 读到第一个满足条件的记录的时候，就是我们需要的最小值了。可以通过查看 `SHOW STATUS` 的全表扫描计数器来验证这点。

一个曲线的优化方法是移除 `MIN()`，然后使用 `LIMIT` 来重写查询：

```mysql
SELECT actor_id
FROM actor
USE INDEX (PRIMARY)
WHERE first_name = 'PENELOPE'
LIMIT 1;
```

这个 SQL 已经无法表达她的本意了。

一般我们通过 SQL 告诉服务器我们需要什么数据，由服务器来决定如何最优地获取数据。

有时候为了获得更高的性能，我们不得不放弃一些原则。

#### 4.5.9. 在同一个表上查询和更新

MySQL 不允许对同一张表同时进行查询和更新。

```mysql
-- 书上没有给表的定义，根据上下文 SQL 自行添加
DROP TABLE IF EXISTS tbl;
CREATE TABLE tbl (
  id   INTEGER AUTO_INCREMENT PRIMARY KEY,
  type TINYINT,
  cnt  INTEGER DEFAULT 0
);

UPDATE tbl AS outer_tbl
SET cnt = (
  SELECT count(*)
  FROM tbl AS inner_tbl
  WHERE inner_tbl.type = outer_tbl.type
);  
# 报错 “[HY000][1093] You can’t specify target table 'outer_tbl' for update in FROM clause”
UPDATE tbl
  INNER JOIN (
               SELECT
                 type,
                 count(*) AS cnt
               FROM tbl
               GROUP BY type
             ) AS der USING (type)
SET tbl.cnt = der.cnt;
# 通过使用生成表的形式来绕过上面的限制。
```

### 4.6. 查询优化器的提示（hint）

如果对查询优化器选择的执行计划不满意，可以使用优化器提供的几个提示来控制最终的执行计划。

- HIGH_PRIORITY 和 LOW_PRIORITY

  当多个语句同事访问某一个表的时候，哪些语句的优先级相对高些、哪些语句的优先级相对低些。这两个提示只对使用表锁的存储引擎有效，千万不要在 InnoDB 或者其他有细粒度锁机制和并发控制的引擎中使用。

- DELAYED

  只对 `INSERT` 和 `REPLACE` 有效。MySQL 会将使用该提示的语句立即返回给客户端，并将插入的行数据放入到缓冲区，然后在表空闲时批量将数据写入。并不是所有的存储引擎都支持；该提示会导致函数 `LAST_INSERT_ID()` 无法正常工作。

- STRAIGHT_JOIN

  放置在 `SELECT` 语句的 `SELECT` 关键字之后：是让查询中所有的表按照在语句中出现的顺序进行关联；放置在任何两个关联表的名字之间：固定其前后两个表的关联顺序。

- SQL_SMALL_RESULT 和 SQL_BIG_RESULT

  只对 `SELECT` 语句有效。告诉优化器对 `GROUP BY` 和 `DISTINCT` 查询如何使用临时表及排序。`SQL_SMALL_RESULT` 告诉优化器结果集很小，可以将结果集放在内存中的索引临时表，以避免排序操作。 `SQL_BIG_RESULT` 告诉优化器结果集可能会非常大，建议使用磁盘临时表做排序操作。

- SQL_BUFFER_RESULT

  告诉优化器将查询结果放入到一个临时表，然后尽可能快地释放表锁。使用服务端缓存无须在客户端上消耗太多内存，可以尽快释放对应的表锁。代价是，服务器端需要更多的内存。

- SQL_CACHE 和 SQL_NO_CACHE

  告诉 MySQL 这个结果集释放应该缓存在查询缓存中。

- SQL_CALC_FOUND_ROWS

  让 MySQL 返回的结果集包含更多的信息。查询中加上该提示 MySQL 会计算除去 `LIMIT` 子句后这个查询要返回的结果集的总数，而实际上只返回 `LIMIT` 要求的结果集。可以通过函数 `FOUND_ROW()` 获取这个值。

- FOR UPDATE 和 LOCK IN SHARE MODE

  主要控制 `SELECT` 语句的锁机制，但只对实现了行级锁的存储引擎有效。该提示会对符合查询条件的数据行加锁。对 `INSERT…SELECT` 语句在 MySQL 5.0 和更新版本会默认给这些记录加上锁。唯一内置的支持这两个提示的引擎就是 InnoDB。这两个提示会让某些优化无法正常使用，例如索引覆盖扫描。InnoDB 不能在不访问主键的情况下排他性地锁定行，因为行的版本信息保存在主键中。

- USE INDEX、IGNORE INDEX 和 FORCE INDEX

  告诉优化器使用或者不使用哪些索引来查询记录。在 MySQL 5.1 和之后的版本可以通过新增选项 `FOR ORDER BY` 和 `FOR GROUP BY` 来指定是否对排序和分组有效。

在 MySQL 5.0 和更新版本中，新增了一些参数用来控制优化器的行为：

- optimizer_search_depth

  控制优化器在穷举执行计划时的限度。如果查询长时间处于 “Statistics” 状态，那么可以考虑调低此参数。

- optimizer_prune_level

  默认打开。让优化器根据需要扫描的行数来决定是否跳过某些执行计划。

- optimizer_switch

  包含了一些开启/关闭优化器特性的标志位。

> MySQL 升级后的验证
>
> 在优化器面前耍一些“小聪明”是不好的。设置的“优化器提示”很可能会让新版的优化策略失效。
>
> 在 MySQL 5.6 中，优化器的改进也是近些年来最大的一次改进。
>
> 升级操作建议仔细检查各个细节，以防止一些边界情况影响你的应用程序。
>
> 使用 Percona Toolkit 中的 `pt-upgrade` 工具，就可以检查在新版中运行的 SQL 是否与老版本一样，返回相同的结果。



### 4.7. 优化特定类型的查询

#### 4.7.1. 优化 `COUNT()` 查询

##### 4.7.1.1. `COUNT()` 的作用

`COUNT()` 是一个特殊的函数，有两种非常不同的作用：可以统计某个列值的数量，也可以统计行数。在统计列值时要求是非空的（不统计 `NULL`）。

当 MySQL 确认括号内的表达式值不可能为空时，实际就是在统计行数。 `COUNT(*)` 不会扩展成所有的列；它会忽略所有的列而直接统计所有的行数。

一个常见错误是：在括号内指定了一个列却希望统计结果集的行数。如果统计结果集的行数，最好使用 `COUNT(*)` ，意义清晰，性能也很好。

##### 4.7.1.2. 关于 MyISAM 的神话

一个容易产生的误解：MyISAM 的 `COUNT()` 函数总是非常快，不过这是有前提条件的，即只有没有任何 `WHERE` 条件的 `COUNT(*)` 才非常快。MySQL 利用存储引擎的特性直接获取这个值。

如果 MySQL 知道某个列 col 不可能为 `NULL` 值，那么 MySQL 内部会将 `COUNT(col)` 表达式转化为 `COUNT(*)`。

当统计带 `WHERE` 子句的结果集行数，可以是统计某个列值的数量时，MyISAM 的 `COUNT()` 和其他存储引擎没有任何不同。

##### 4.7.1.3. 简单的优化

```mysql
-- 书中没有建表语句，根据上下文 SQL 创建
DROP TABLE IF EXISTS city;
CREATE TABLE city (
  id   INTEGER AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL
) ENGINE = MyISAM;

-- 没有优化的 SQL，需要扫描大多数行
SELECT COUNT(*)
FROM city
WHERE id > 5;

-- 优化后的 SQL，只需要扫描少量的行
SELECT (SELECT COUNT(*)
        FROM city) - COUNT(*)
FROM city
WHERE id <= 5;
```

在同一个查询中统计同一列的不同值的数量，以减少查询的语句量。可以这样：

```mysql
DROP TABLE IF EXISTS items;
CREATE TABLE items (
  id    INTEGER AUTO_INCREMENT PRIMARY KEY,
  color VARCHAR(50)
);

SELECT
  SUM(IF(color = 'blue', 1, 0)) AS blue,
  SUM(IF(color = 'red', 1, 0))  AS red
FROM items;

SELECT
  COUNT(color = 'blue' OR NULL) AS blue,
  COUNT(color = 'red' OR NULL)  AS red
FROM items;
```

##### 4.7.1.4. 使用近似值

有时候某些业务场景并不要求完全精确的 `COUNT` 值，此时可以用近似值代替。`EXPLAIN` 出来的优化器估算的行数就是一个不错的近似值，执行 `EXPLAIN` 并不需要真正地去执行查询，所以成本很低。

很多时候，计算精确值的成本非常高，而计算近似值则非常简单。例如统计网站的当前活跃用户数。

##### 4.7.1.5. 更复杂的优化

通常来说， `COUNT()` 都需要扫描大量的行才能获取精确的结果，因此很难优化。除了上面的方法，还可以使用索引覆盖扫描。

如果这还不够，就需要考虑修改应用的架构，可以增加汇总表，或者增加类似 Memcached 这样的外部缓存系统。很快发现陷入一个困境，“快速，精确和实现简单”，三者永远只能满足其二，必须舍掉其中之一。

#### 4.7.2. 优化关联查询

- 确保 `ON` 或者 `USING` 子句的列上有索引。一般来说，除非有其他理由，否则只需要在关联顺序中的第二个表的相应列上创建索引。
- 确保任何的 `GROUP BY` 和 `ORDER BY` 中的表达式只涉及到一个表中的列，这样 MySQL 才有可能使用索引来优化这个过程。
- 当升级 MySQL 的时候需要注意：关联语法、运算符优先级等其他可能会发生变化的地方。

#### 4.7.3. 优化子查询

子查询优化最重要的优化建议是尽可能使用关联查询代替，至少当前的 MySQL 版本需要这样。

使用 MySQL 5.6 或者更新的版本或者 MariaDB，则可以忽略这个建议。

#### 4.7.4. 优化 `GROUP BY` 和 `DISTINCT`

它们都可以使用索引来优化，这也是最有效的优化办法。

在 MySQL 中，当无法使用索引的时候， `GROUP BY` 使用两种策略来完成：使用临时表或者文件排序来分组。

如果需要对关联查询做分组，并且是按照查找表中的某个列进行分组，那么通常采用查找表的标识列分组的效果会比其他列更高。例如：

```mysql
-- 这个查询效率不会很好
SELECT
  actor.first_name,
  actor.last_name,
  COUNT(*)
FROM film_actor
  INNER JOIN actor USING (actor_id)
GROUP BY actor.first_name, actor.last_name;

-- 这个查询的效率更高
SELECT
  actor.first_name,
  actor.last_name,
  COUNT(*)
FROM film_actor
  INNER JOIN actor USING (actor_id)
GROUP BY film_actor.actor_id;
```

建议始终使用**含义明确的语法。**

如果密钥通过 `ORDER BY` 子句显式地指定拍序列，当查询使用 `GROUP BY` 子句的时候，结果集会自动按照分组的字段进行排序。如果不关心结果集的顺序，则可以使用 `ORDER BY NULL`，让 MySQL 不再进行文件排序。也可以在 `GROUP BY` 子句中直接使用 `DESC` 和 `ASC` 关键字，使分组的结果集按需要的方向排序。

##### 4.7.4.1. 优化 `GROUP BY WITH ROLLUP`

分组查询的一个变种就是要求 MySQL 对返回的分组结果再做一次超级聚合。可以使用 `WITH ROLLUP` 子句来实现，但可能不够优化。

最好的办法是尽可能的将 `WITH ROLLUP` 功能转移到应用程序中处理。

#### 4.7.5. 优化 `LIMIT` 分页

一个非常常见又令人头疼的问题就是，在偏移量非常大的时候，查询代价非常高。要优化这种查询，要么是在页面中限制分页的数量，要么是优化大偏移量的性能。

优化此类分页查询的一个最简单的办法就是尽可能使用索引覆盖扫描，而不是查询所有的列。然后根据需要做一次关联操作再返回所需要的列。对于偏移量很大的时候，这样做的效率会提升非常大。

```MYSQL
-- 效率一般
SELECT
  film_id,
  description
FROM film
ORDER BY title
LIMIT 50, 5;

-- 延迟关联，大大提升查询效率
SELECT
  film_id,
  description
FROM film
  INNER JOIN (
       SELECT film_id
       FROM film
       ORDER BY title
       LIMIT 50, 5
     ) AS lim USING (film_id);

```

有时候也可以将 `LIMIT` 查询转换为已知位置的查询，让 MySQL 通过范围扫描获得到对应的结果。

`LIMIT` 和 `OFFSET` 的问题，其实是 `OFFSET` 的问题，它会导致 MySQL 扫描大量不需要的行然后在抛弃掉。如果可以使用书签记录上次数据的位置，那么下次就可以直接从该书签记录的位置开始扫描，这样就可以避免使用 `OFFSET`。

其他优化方法还包括使用预先计算的汇总表，或者关联到一个冗余表，冗余表值包含主键列和需要做排序的数据列。

#### 4.7.6. 优化 `SQL_CALC_FOUND_ROWS`

分页的时候，另外一个常用的技巧是在 `LIMIT` 语句中加上 `SQL_CALC_FOUND_ROWS` 提示，这样就可以获得去掉 `LIMIT` 以后满足条件的行数，因此可以作为分页的总数。加上该提示，MySQL 都会扫描所有满足条件的行再抛弃不需要的行，代价非常高。

一个更好的设计是将具体的页数换成“下一页”按钮，这样只需要下一页的是否有数据，就决定是否显示“下一页”按钮。

另外一种做法是先获取并缓存较多的数据，然后每次分页都从这个缓存中获取。

有时候也可以考虑使用 `EXPLAIN` 的结果中的 `rows` 列的值作为结果集总数的近似值。当需要使用精确值时，再单独使用 `COUNT(*)` 来满足需求。

#### 4.7.7. 优化 `UNION` 查询

MySQL 总是通过创建并填充临时表的方式来执行 `UNION` 查询。经常需要手动将 `WHERE`、`LIMIT`、`ORDER BY` 等子句下推到 `UNION` 的各个子查询中，以便优化器可以充分利用这些条件进行优化。

除非确实需要服务器消除重复的行，否则就一定要使用 `UNION ALL`。

#### 4.7.8. 静态查询分析

Percona Toolkit 中的 `pt-query-advisor` 能够解析查询日志、分析查询模式，然后给出所有可能存在潜在问题的查询，并给出足够详细的建议。

#### 4.7.9. 使用用户自定义变量

用户自定义变量是一个用来存储内容的临时容器，在连接 MySQL 的整个过程中都存在。

不能使用用户自定义变量的场景：

- 使用自定义变量的查询，无法使用查询缓存。
- 不能再使用常量或者标识符的地方使用自定义变量，例如表名等。
- 用户自定义变量的生命周期是在一个连接中有效，所以不能用它们来做连接间的通信。
- 如果使用连接池或者持久化连接，自定义变量可能让看起来毫无关系的代码发生交互。
- 在 5.0 之前的版本，是大小写敏感的。
- 不能显式地声明自定义变量的类型。
- MySQL 优化器在某些场景下可能会将这些变量优化掉。
- 赋值的顺序和赋值的时间点并不总是固定的，这依赖于优化器的决定。
- 赋值符号 `:=` 的优先级非常低。
- 使用未定义变量不会产生任何语法错误。

##### 4.7.9.1. 优化排名语句

使用用户自定义变量的一个重要特性是可以在给一个变量赋值的同时使用这个变量。

代码 15. 使用变量显示行号

```mysql
SET @rownum := 0;
SELECT
  actor_id,
  @rownum := @rownum + 1 AS rownum
FROM actor
LIMIT 3;
```

代码 16. 使用变量排序，相同数量排名也相同

```mysql
SET @curr_cnt := 0, @prev_cnt := 0, @rank := 0;

SELECT
  actor_id,
  @curr_cnt := cnt                                          AS cnt,
  @rank     := if(@prev_cnt <> @curr_cnt, @rank + 1, @rank) AS rank,
  @prev_cnt := @curr_cnt                                    AS dummy
FROM (
   SELECT
     actor_id,
     COUNT(*) AS cnt
   FROM film_actor
   GROUP BY actor_id
   ORDER BY cnt DESC
   LIMIT 10
) AS der;
```

##### 4.7.9.2. 避免重复查询刚刚更新的数据

```mysql
-- 根据上下文推断的建表语句
DROP TABLE IF EXISTS tbl;
CREATE TABLE tbl (
  id          INTEGER AUTO_INCREMENT PRIMARY KEY,
  lastupdated TIMESTAMP
);

-- 常规做法
UPDATE tbl SET tbl.lastupdated = NOW() WHERE id = 1;
SELECT lastupdated FROM tbl WHERE id = 1;

-- 使用变量，无须访问数据表，更高效
UPDATE tbl SET tbl.lastupdated = NOW() WHERE id = 1 AND @now := NOW();
SELECT @now;
```

##### 4.7.9.3. 确定取值的顺序

使用用户自定义变量的一个最常见的问题是没有注意到在赋值和读取变量的时候可能是在查询的不同阶段。例如，在 `SELECT` 中定义，在 `WHERE` 中使用。

解决这个问题的办法是让变量的赋值和取值发生在执行查询的同一阶段。

```mysql
SET @rownum := 0;
SELECT
  actor_id,
  @rownum AS rownum
FROM actor
WHERE (@rownum := @rownum + 1) <= 1;

```

一个技巧：将赋值语句放到 `LEAST()` 函数中，这样就可以在完全不改变顺序的时候完成赋值操作。

```mysql
SET @rownum := 0;
SELECT
  actor_id,
  first_name,
  @rownum AS rownum
FROM actor
WHERE @rownum <= 1
ORDER BY first_name, LEAST(0, @rownum := @rownum + 1);

```

##### 4.7.9.4. 编写偷懒的 `UNION`

将用户分为热门用户和归档用不。查询用户时，热门用户中查不出来才去查归档用户，避免不必要的 `UNION` 子查询。

```mysql
-- 建表语句是根据上下文推断的
DROP TABLE IF EXISTS users;
CREATE TABLE users (
  id INTEGER AUTO_INCREMENT PRIMARY KEY
);
DROP TABLE IF EXISTS users_archived;
CREATE TABLE users_archived (
  id INTEGER AUTO_INCREMENT PRIMARY KEY
);

-- 查询用户，热门用户中查不出来则查归档用户
SELECT
  greatest(@found := -1, id) AS id,
  'users'                    AS which_tbl
FROM users
WHERE id = 1

UNION ALL

SELECT
  id,
  'users_archived' AS which_tbl
FROM users_archived
WHERE id = 1 AND @found IS NULL

UNION ALL
-- 将变量充值，避免影响下次查询
SELECT
  1,
  'reset'
FROM dual
WHERE (@found := NULL) IS NOT NULL;
```

##### 4.7.9.5. 用户自定义变量的其他用处

在任何类型的 SQL 语句中都可以对变量进行赋值。

一些典型的使用场景：

- 查询运行时计算总数和平均值。
- 模拟 `GROUP` 语句中的函数 `FIRST()` 和 `LAST()`。
- 对大量数据做一些数据计算。
- 计算一个大表的 MD5 散列值。
- 编写一个样本处理函数，当样本中的数值超过某个边界值的时候将其变成0。
- 模拟读/写游标。
- 在 `SHOW` 语句的 `WHERE` 子句中加入变量值。

推荐阅读 [SQL and Relational Theory](https://book.douban.com/subject/26665768/)，改变对 SQL 语句的认识。

### 4.8. 案例学习

*待补充*

#### 4.8.1. 使用 MySQL 构建一个队列表

#### 4.8.2. 计算两点之间的距离

#### 4.8.3. 使用用户定义函数

### 4.9. 总结

要想写一个好的查询，你必须理解 Schema 设计、索引设计等，反之亦然。

优化通常都需要三管齐下：不做、少做、快速地做。

## 5. 分库分表分片

### 5.1. 问题分析

1. 主从复制

2. 读写分离

3. 分库分表

   1. 水平拆分

      某个字段按一定规律进行拆分，将一个表的数据分到多个表（库）中降低表的数据量，优化查询数据量的方式，来提高性能。

      - 特点

        ①.每个库（表）的结构都一样。②.每个库（表）的数据都不一样。③.每个库（表）的并集是整个数据库的全量数据 。

      - 分库分表常见算法

        ①.Hash取模：通过表的一列字段进行hash取出code值来区分的。（不好迁移） ②.Range范围： 按年份、按时间。（不好查找，如果找个数据没有时间，需要全部找） ③.List预定义：事先设定100找。（判断需要建立多少个分库）

      - 解决问题

        单表中数据量增长出现的压力。

      - 不解决问题

        表与表之间的io争夺。

      - 分库分表之后带来的问题

        ①.查询数据结果集合并。②.sql的改变。③.分布式事务。④.全局唯一性id。

   2. 垂直拆分

      将一个字段（属性）比较多的表拆分成多个小表，将不同字段放到不同的表中降低单（表）库大小的目的来提高性能。

      - 通俗

        大表拆小表，拆分是基于关系型数据库的列（字段）来进行

      - 特点

        ①. 每个库（表）的结构都不一样。②.每个库（表）数据都（至少有一列）一样。③.每个库（表）的并集是整个数据库的全量数据。④.每个库（表）的数据量（count）不会变的。

      - 解决问题

        表与表之间的io竞争。

      - 不解决问题

        单表中数据量增长出现的压力。

没想到，无意间想到的一个点子（把大表拆分成两个表，不常用字段单独存储）竟然符合了垂直拆分的套路。

垂直拆分竟然可以分成两层面来搞：大的角度，按照业务拆分成商品、用户等多个模块；小的方面，把一张大表拆分成多个小表。

### 5.2. Sharding Sphere

#### 5.2.1. 分片的核心概念

- 逻辑表

  水平拆分的数据库（表）的相同逻辑和数据结构表的总称。例：订单数据根据主键尾数拆分为10张表，分别是torder0到torder9，他们的逻辑表名为t_order。

- 真实表

  在分片的数据库中真实存在的物理表。即上个示例中的torder0到torder9。

- 数据节点

  数据分片的最小单元。由数据源名称和数据表组成，例：ds0.torder_0。

- 绑定表

  指分片规则一致的主表和子表。例如：torder表和torderitem表，均按照orderid分片，则此两张表互为绑定表关系。绑定表之间的多表关联查询不会出现笛卡尔积关联，关联查询效率将大大提升。

- 广播表

  指所有的分片数据源中都存在的表，表结构和表中的数据在每个数据库中均完全一致。适用于数据量不大且需要与海量数据的表进行关联查询的场景，例如：字典表。

- 逻辑索引

  某些数据库（如：PostgreSQL）不允许同一个库存在名称相同索引，某些数据库（如：MySQL）则允许只要同一个表中不存在名称相同的索引即可。 逻辑索引用于同一个库不允许出现相同索引名称的分表场景，需要将同库不同表的索引名称改写为索引名 + 表名，改写之前的索引名称成为逻辑索引。

#### 5.2.2. 分片

- 分片键

  用于分片的数据库字段，是将数据库(表)水平拆分的关键字段。例：将订单表中的订单主键的尾数取模分片，则订单主键为分片字段。 SQL中如果无分片字段，将执行全路由，性能较差。 除了对单分片字段的支持，ShardingSphere也支持根据多个字段进行分片。

- 分片算法

  通过分片算法将数据分片，支持通过=、BETWEEN和IN分片。分片算法需要应用方开发者自行实现，可实现的灵活度非常高。精确分片算法 — 对应PreciseShardingAlgorithm，用于处理使用单一键作为分片键的=与IN进行分片的场景。需要配合StandardShardingStrategy使用。范围分片算法 — 对应RangeShardingAlgorithm，用于处理使用单一键作为分片键的BETWEEN AND进行分片的场景。需要配合StandardShardingStrategy使用。复合分片算法 — 对应ComplexKeysShardingAlgorithm，用于处理使用多键作为分片键进行分片的场景，包含多个分片键的逻辑较复杂，需要应用开发者自行处理其中的复杂度。需要配合ComplexShardingStrategy使用。Hint分片算法 — 对应HintShardingAlgorithm，用于处理使用Hint行分片的场景。需要配合HintShardingStrategy使用。

- 分片策略

  包含分片键和分片算法，由于分片算法的独立性，将其独立抽离。真正可用于分片操作的是分片键 + 分片算法，也就是分片策略。目前提供5种分片策略。标准分片策略 — 对应StandardShardingStrategy。提供对SQL语句中的=, IN和BETWEEN AND的分片操作支持。StandardShardingStrategy只支持单分片键，提供PreciseShardingAlgorithm和RangeShardingAlgorithm两个分片算法。PreciseShardingAlgorithm是必选的，用于处理=和IN的分片。RangeShardingAlgorithm是可选的，用于处理BETWEEN AND分片，如果不配置RangeShardingAlgorithm，SQL中的BETWEEN AND将按照全库路由处理。复合分片策略 — 对应ComplexShardingStrategy。复合分片策略。提供对SQL语句中的=, IN和BETWEEN AND的分片操作支持。ComplexShardingStrategy支持多分片键，由于多分片键之间的关系复杂，因此并未进行过多的封装，而是直接将分片键值组合以及分片操作符透传至分片算法，完全由应用开发者实现，提供最大的灵活度。行表达式分片策略 — 对应InlineShardingStrategy。使用Groovy的表达式，提供对SQL语句中的=和IN的分片操作支持，只支持单分片键。对于简单的分片算法，可以通过简单的配置使用，从而避免繁琐的Java代码开发，如: tuser$→{uid % 8} 表示tuser表根据uid模8，而分成8张表，表名称为tuser0到tuser_7。Hint分片策略 — 对应HintShardingStrategy。通过Hint而非SQL解析的方式分片的策略。不分片策略 — 对应NoneShardingStrategy。不分片的策略。



![image-20220123204058296](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123204058296-877aa4.png)

图 44. 路由规则

![image-20220123204119768](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123204119768-8851a0.png)



图 45. 改写规则

![image-20220123204139025](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123204139025-657920.png)



图 46. 改写规则

### 5.3. 参考资料

1. [『互联网架构』软件架构-mysql终级解决方案分库分表（65）](https://mp.weixin.qq.com/s/frdj6vFz24XEimPPQAgnVA)
2. [『互联网架构』软件架构-Sharding-Sphere分库分表（66）](https://mp.weixin.qq.com/s/Ktf__hB6kzZrhar4UG6Nog)
3. [『互联网架构』软件架构-Sharding-Sphere特性详解（67）](https://mp.weixin.qq.com/s/hInARjmbetXDEl0zd_AIEg)

## 6. MySQL `explain` 详解

进行 MySQL 查询优化，`explain` 是必备技能。这章就来重点介绍一下 `explain`。

![image-20220123204217048](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123204217048-7d1b49.png)



图 47. SQL Joins

### 6.1. 示例数据库

为了方便后续讲解，这里使用 MySQL 官方提供的示例数据库： [MySQL : Sakila Sample Database](https://dev.mysql.com/doc/sakila/en/)。需求的小伙伴，请到官方页面下载并安装。

Sakila 库的 Schema 设计图如下：

![image-20220123204249088](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123204249088-4c48cd.png)



图 48. Sakila Sample Database



![image-20220123204308803](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123204308803-c290f7.png)

图 49. Sakila Sample Database

![image-20220123204325974](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123204325974-0330f9.png)

图 50. Sakila Sample Database

### 6.2. `EXPLAIN` 语法

`DESCRIBE` 和 `EXPLAIN` 是同义词。在实践中，`DESCRIBE` 多用于显示表结构，而 `EXPLAIN` 多用于显示 SQL 语句的执行计划。

```mysql
{EXPLAIN | DESCRIBE | DESC}
    tbl_name [col_name | wild]

{EXPLAIN | DESCRIBE | DESC}
    [explain_type]
    {explainable_stmt | FOR CONNECTION connection_id}

explain_type: {
    EXTENDED
  | PARTITIONS
  | FORMAT = format_name
}

format_name: {
    TRADITIONAL
  | JSON
}

explainable_stmt: {
    SELECT statement
  | DELETE statement
  | INSERT statement
  | REPLACE statement
  | UPDATE statement
}
```

这里有一点说明一下，默认情况下，`EXPLAIN` 的结果类似于普通的 `SELECT` 查询语句的表格输出。也可以通过将结果指定为 JSON 格式的，例如：

```mysql
EXPLAIN FORMAT = JSON
SELECT *
FROM actor
WHERE actor_id = 1;

```

### 6.3. `DESCRIBE` 获取表结构

`DESCRIBE` 是 `SHOW COLUMNS` 的简写形式。

```mysql
mysql> DESCRIBE actor;
+-------------+-------------------+------+-----+-------------------+-----------------------------------------------+
| Field       | Type              | Null | Key | Default           | Extra                                         |
+-------------+-------------------+------+-----+-------------------+-----------------------------------------------+
| actor_id    | smallint unsigned | NO   | PRI | NULL              | auto_increment                                |
| first_name  | varchar(45)       | NO   |     | NULL              |                                               |
| last_name   | varchar(45)       | NO   | MUL | NULL              |                                               |
| last_update | timestamp         | NO   |     | CURRENT_TIMESTAMP | DEFAULT_GENERATED on update CURRENT_TIMESTAMP |
+-------------+-------------------+------+-----+-------------------+-----------------------------------------------+
```

### 6.4. `SHOW PROFILES` 显示执行时间

在 MySQL 数据库中，可以通过配置 `profiling` 参数来启用 SQL 剖析。

```mysql
-- 查看是否开启
SHOW VARIABLES LIKE '%profil%';

-- 开启
SET profiling = ON;
SET profiling_history_size = 100;

-- 查看帮助
HELP PROFILE;
# 建议在 MySQL 命令行工具中使用，否则会混入很多乱七八糟的查询语句。
SHOW PROFILES; 

SHOW PROFILE;
# 在 DataGrip 中还不支持。
SHOW PROFILE FOR QUERY 119; 

--查看特定部分的开销，如下为CPU部分的开销
SHOW PROFILE CPU FOR QUERY 119;

--如下为MEMORY部分的开销
SHOW PROFILE MEMORY FOR QUERY 119;

--同时查看不同资源开销
SHOW PROFILE BLOCK IO, CPU FOR QUERY 119;

--显示SWAP的次数。
SHOW PROFILE SWAPS FOR QUERY 119;
```

结合使用情况来看，这个语句的结果并不稳定，同一条语句多次查询，返回的结果相差很大。

另外，值得一提的是，这个工具在官方文档中已经指出，未来将会被移除，请使用 [Query Profiling Using Performance Schema](https://dev.mysql.com/doc/refman/8.0/en/performance-schema-query-profiling.html) 代替。

### 6.5. `EXPLAIN` 输出

#### 6.5.1. `id`

`SELECT` 标识符，SQL 执行的顺序的标识，SQL 从大到小的执行。如果在语句中没子查询或关联查询，只有唯一的 `SELECT`，每行都将显示 `1`。否则，内层的 `SELECT` 语句一般会顺序编号，对应于其在原始语句中的位置

- `id` 相同时，执行顺序由上至下
- 如果是子查询，`id` 的序号会递增，`id` 值越大优先级越高，越先被执行
- 如果 `id` 相同，则认为是一组，从上往下顺序执行；在所有组中，`id` 值越大，优先级越高，越先执行

#### 6.5.2. `select_type`

##### 6.5.2.1. `SIMPLE`

简单 `SELECT`，不使用 `UNION` 或子查询等

##### 6.5.2.2. `PRIMARY`

查询中若包含任何复杂的子部分,最外层的select被标记为PRIMARY

##### 6.5.2.3. `UNION`

UNION中的第二个或后面的SELECT语句

##### 6.5.2.4. `DEPENDENT UNION`

UNION中的第二个或后面的SELECT语句，取决于外面的查询

##### 6.5.2.5. `UNION RESULT`

UNION的结果

##### 6.5.2.6. `SUBQUERY`

子查询中的第一个SELECT

##### 6.5.2.7. `DEPENDENT SUBQUERY`

子查询中的第一个SELECT，取决于外面的查询

##### 6.5.2.8. `DERIVED`

派生表的SELECT, FROM子句的子查询

##### 6.5.2.9. `DEPENDENT DERIVED`

派生表的SELECT, FROM子句的子查询 `MATERIALIZED`::

##### 6.5.2.10. `UNCACHEABLE SUBQUERY`

一个子查询的结果不能被缓存，必须重新评估外链接的第一行

##### 6.5.2.11. `UNCACHEABLE UNION`

？？

#### 6.5.3. `table`

访问引用哪个表（例如下面的 `actor`）：

```mysql
EXPLAIN
SELECT *
FROM actor
WHERE actor_id = 1;

```

#### 6.5.4. `partitions`

#### 6.5.5. `type`

`type` 显示的是数据访问类型，是较为重要的一个指标，结果值从好到坏依次是： `system` > `const` > `eq_ref` > `ref` > `fulltext` > `ref_or_null` > `index_merge` > `unique_subquery` > `index_subquery` > `range` > `index` > `ALL`。一般来说，得保证查询至少达到 `range` 级别，最好能达到 `ref`。

##### 6.5.5.1. `system`

当 MySQL 对查询某部分进行优化，并转换为一个常量时，使用这些类型访问。如将主键置于 `WHERE` 列表中，MySQL 就能将该查询转换为一个常量。`system` 是 `const` 类型的特例，当查询的表只有一行的情况下，使用 `system`。

##### 6.5.5.2. `const`

在查询开始时读取，该表最多有一个匹配行。因为只有一行，所以这一行中的列的值可以被其他优化器视为常量。`const` 表非常快，因为它们只读取一次。

```mysql
EXPLAIN
SELECT *
FROM actor
WHERE actor_id = 1;

```

##### 6.5.5.3. `eq_ref`

类似 `ref`，区别就在使用的索引是唯一索引，对于每个索引键值，表中只有一条记录匹配，简单来说，就是多表连接中使用 `PRIMARY KEY` 或者 `UNIQUE KEY` 作为关联条件

最多只返回一条符合条件的记录。使用唯一性索引或主键查找时会发生（高效）。

##### 6.5.5.4. `ref`

表示上述表的连接匹配条件，即哪些列或常量被用于查找索引列上的值

一种索引访问，它返回所有匹配某个单个值的行。此类索引访问只有当使用非唯一性索引或唯一性索引非唯一性前缀时才会发生。这个类型跟 `eq_ref` 不同的是，它用在关联操作只使用了索引的最左前缀，或者索引不是 `UNIQUE` 和 `PRIMARY KEY`。`ref` 可以用于使用 `=` 或 `<⇒` 操作符的带索引的列。

```mysql
EXPLAIN
SELECT *
FROM address
WHERE city_id = 119;

```

##### 6.5.5.5. `fulltext`

全文检索

```mysql
EXPLAIN
SELECT *
FROM film_text
WHERE MATCH(title, description) AGAINST('ACE')
LIMIT 100;
```

##### 6.5.5.6. `ref_or_null`

MySQL在优化过程中分解语句，执行时甚至不用访问表或索引，例如从一个索引列里选取最小值可以通过单独索引查找完成。

##### 6.5.5.7. `index_merge`

##### 6.5.5.8. `unique_subquery`

##### 6.5.5.9. `index_subquery`

##### 6.5.5.10. `range`

范围扫描，一个有限制的索引扫描。`key` 列显示使用了哪个索引。当使用 `=`、 `<>`、`>`、`>=`、`<`、`⇐`、`IS NULL`、`<⇒`、`BETWEEN` 或者 `IN` 操作符，用常量比较关键字列时,可以使用 `range`。

```mysql
EXPLAIN
SELECT *
FROM actor
WHERE actor_id > 100;

```

##### 6.5.5.11. `index`

Full Index Scan，`index` 与 `ALL` 区别为 `index` 类型只遍历索引树。和全表扫描一样。只是扫描表的时候按照索引次序进行而不是行。主要优点就是避免了排序, 但是开销仍然非常大。如在 `Extra` 列看到 `Using index`，说明正在使用覆盖索引，只扫描索引的数据，它比按索引次序全表扫描的开销要小很多

##### 6.5.5.12. `ALL`

Full Table Scan，最坏的情况，全表扫描，MySQL 将遍历全表以找到匹配的行。

```mysql
EXPLAIN
SELECT *
FROM actor;

```

#### 6.5.6. `possible_keys`

显示查询使用了哪些索引，表示该索引可以进行高效地查找，但是列出来的索引对于后续优化过程可能是没有用的。

#### 6.5.7. `key`

`key` 列显示 MySQL 实际决定使用的键（索引）。如果没有选择索引，键是 `NULL`。要想强制 MySQL 使用或忽视 `possible_keys` 列中的索引，在查询中使用 `FORCE INDEX`、`USE INDEX` 或者 `IGNORE INDEX`。

#### 6.5.8. `key_len`

`key_len` 列显示 MySQL 决定使用的键长度。如果键是 `NULL`，则长度为 `NULL`。使用的索引的长度。在不损失精确性的情况下，长度越短越好 。

#### 6.5.9. `ref`

`ref` 列显示使用哪个列或常数与 `key` 一起从表中选择行。

#### 6.5.10. `rows`

`rows` 列显示 MySQL 认为它执行查询时必须检查的行数。注意这是一个预估值。

#### 6.5.11. `filtered`

给出了一个百分比的值，这个百分比值和 rows 列的值一起使用。(5.7才有)

#### 6.5.12. `Extra`

`Extra` 是 `EXPLAIN` 输出中另外一个很重要的列，该列显示 MySQL 在查询过程中的一些详细信息，MySQL 查询优化器执行查询的过程中对查询计划的重要补充信息。

##### 6.5.12.1. `Child of **'table'** pushed join@1`

##### 6.5.12.2. `const row not found`

##### 6.5.12.3. `Deleting all rows`

##### 6.5.12.4. `Distinct`

优化 `DISTINCT` 操作，在找到第一匹配的元组后即停止找同样值的动作

##### 6.5.12.5. `FirstMatch(**tbl_name**)`

##### 6.5.12.6. `Full scan on NULL key`

##### 6.5.12.7. `Impossible HAVING`

##### 6.5.12.8. `Impossible WHERE`

```mysql
EXPLAIN
SELECT *
FROM actor
WHERE actor_id IS NULL;

```

因为 `actor_id` 是 `actor` 表的主键。所以，这个条件不可能成立。

##### 6.5.12.9. `Impossible WHERE noticed after reading const tables`

##### 6.5.12.10. `LooseScan(**m..n**)`

##### 6.5.12.11. `No matching min/max row`

##### 6.5.12.12. `no matching row in const table`

##### 6.5.12.13. `No matching rows after partition pruning`

##### 6.5.12.14. `No tables used`

##### 6.5.12.15. `Not exists`

MySQL 优化了 `LEFT JOIN`，一旦它找到了匹配 `LEFT JOIN` 标准的行， 就不再搜索了。

##### 6.5.12.16. `Plan isn’t ready yet`

##### 6.5.12.17. `Range checked for each record (index map: **N**)`

##### 6.5.12.18. `Recursive`

##### 6.5.12.19. `Rematerialize`

##### 6.5.12.20. `Scanned **N** databases`

##### 6.5.12.21. `Select tables optimized away`

在没有 `GROUP BY` 子句的情况下，基于索引优化 `MIN/MAX` 操作，或者对于 MyISAM 存储引擎优化 `COUNT(*)` 操作，不必等到执行阶段再进行计算，查询执行计划生成的阶段即完成优化。

```mysql
EXPLAIN
SELECT MIN(actor_id), MAX(actor_id)
FROM actor;
```

##### 6.5.12.22. `Skip_open_table, Open_frm_only, Open_full_table`

- `Skip_open_table`
- `Open_frm_only`
- `Open_full_table`

##### 6.5.12.23. `Start temporary, End temporary`

##### 6.5.12.24. `unique row not found`

##### 6.5.12.25. `Using filesort`

MySQL 有两种方式可以生成有序的结果，通过排序操作或者使用索引，当 `Extra` 中出现了 `Using filesort` 说明MySQL使用了后者，但注意虽然叫 `filesort` 但并不是说明就是用了文件来进行排序，只要可能排序都是在内存里完成的。大部分情况下利用索引排序更快，所以一般这时也要考虑优化查询了。使用文件完成排序操作，这是可能是 `ordery by`，`group by` 语句的结果，这可能是一个 CPU 密集型的过程，可以通过选择合适的索引来改进性能，用索引来为查询结果排序。

##### 6.5.12.26. `Using index`

说明查询是覆盖了索引的，不需要读取数据文件，从索引树（索引文件）中即可获得信息。如果同时出现 `using where`，表明索引被用来执行索引键值的查找，没有 `using where`，表明索引用来读取数据而非执行查找动作。这是MySQL 服务层完成的，但无需再回表查询记录。

##### 6.5.12.27. `Using index condition`

这是 MySQL 5.6 出来的新特性，叫做“索引条件推送”。简单说一点就是 MySQL 原来在索引上是不能执行如 `like` 这样的操作的，但是现在可以了，这样减少了不必要的 I/O 操作，但是只能用在二级索引上。

##### 6.5.12.28. `Using index for group-by`

##### 6.5.12.29. `Using index for skip scan`

##### 6.5.12.30. `Using join buffer (Block Nested Loop), Using join buffer (Batched Key Access)`

使用了连接缓存：Block Nested Loop，连接算法是块嵌套循环连接；Batched Key Access，连接算法是批量索引连接。

##### 6.5.12.31. `Using MRR`

##### 6.5.12.32. `Using sort_union(…), Using union(…), Using intersect(…)`

##### 6.5.12.33. `Using temporary`

用临时表保存中间结果，常用于 `GROUP BY` 和 `ORDER BY` 操作中，一般看到它说明查询需要优化了，就算避免不了临时表的使用也要尽量避免硬盘临时表的使用。

##### 6.5.12.34. `Using where`

使用了 `WHERE` 从句来限制哪些行将与下一张表匹配或者是返回给用户。注意：`Extra` 列出现 `Using where` 表示MySQL 服务器将存储引擎返回服务层以后再应用 `WHERE` 条件过滤。

```mysql
EXPLAIN
SELECT *
FROM actor
WHERE actor_id > 100;

```

##### 6.5.12.35. `Using where with pushed condition`

##### 6.5.12.36. `Zero limit`

查询有 `LIMIT 0` 子句，所以导致不能选出任何一行。

```mysql
EXPLAIN
SELECT *
FROM actor
LIMIT 0;

```

## 7. `EXPLAIN` 实践

如果 `EXPLAIN` 执行计划的 `Extra` 列包含 “Using temporary”，则说明这个查询使用了隐式临时表。

如果 `EXPLAIN` 执行计划的 `Extra` 列包含 “Using union(XX,YY,ZZ…)”，则说明这个查询使用了索引合并策略，应该检查一下查询和表的结构。 P158

当发起一个被索引覆盖的查询（也叫作索引覆盖查询）时，在 `EXPLAIN` 的 `Extra` 列可以看到 “Using index” 的信息。 P172

很容易把 `Extra` 列的 “Using index” 和 `type` 列的 “index” 搞混淆。其实这两者完全不同， `type` 列和覆盖索引毫无关系；它只是表示这个查询访问数据的方式，或者说是 MySQL 查找行的方式。 MySQL 手册中称之为连接方式（join type）。P172脚注

如果 `EXPLAIN` 出来的 `type` 列的值为 “index”，则说明 MySQL 使用了索引扫描来做排序（不要和 `Extra` 列的 “Using index” 搞混淆）。P175

`EXPLAIN` 的 `Extra` 列出现了 “Using where” 表示 MySQL 服务器将存储引擎返回行以后再应用 `WHERE` 过滤条件。P

从 `EXPLAIN` 的输出很难区分 MySQL 是要查询范围值，还是查询列表值。 `EXPLAIN` 使用同样的词“range”来描述这两种情况。对于范围条件查询， MySQL 无法再使用范围列后面的其他索引列了，但是对于“多个等值条件查询”则没有这个限制。

在 `EXPLAIN` 语句中的 `type` 列反应了访问类型。访问类型有很多种，从全表扫描到索引扫描、范围扫描、唯一索引查询、常数引用等。

在 `EXPLAIN` 语句中的 `type` 列反应了访问类型。访问类型有很多种，从全表扫描到索引扫描、范围扫描、唯一索引查询、常数引用等。P199

第五章 多列索引 如果在 EXPLAIN 中看到有索引合并。如何查看？哪些指标表明这个问题？

取最大值或者最小值时，如果有索引，则可以直接从 B-Tree 索引的两端取数据，在 `EXPLAIN` 中就可以看到 `Select tables optimized away`。从字面意思可以看出，它表示优化器已经从执行计划中移除了该表，并以一个常数取而代之。P211

```mysql
EXPLAIN
SELECT
  f.film_id,
  fa.actor_id
FROM film f
  INNER JOIN film_actor fa USING (film_id)
WHERE f.film_id = 1 \G

*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: f
   partitions: NULL
         type: const
possible_keys: PRIMARY
          key: PRIMARY
      key_len: 2
          ref: const
         rows: 1
     filtered: 100.00
        Extra: Using index
*************************** 2. row ***************************
           id: 1
  select_type: SIMPLE
        table: fa
   partitions: NULL
         type: ref
possible_keys: idx_fk_film_id
          key: idx_fk_film_id
      key_len: 2
          ref: const
         rows: 10
     filtered: 100.00
        Extra: Using index
```

MySQL 分两步来执行查询。第一步从 `film` 表找到需要的行。因为在 `film_id` 字段上有主键索引，所以 MySQL 优化器知道这只会返回一行数据，优化器在生成执行计划的时候，就已经通过索引信息知道将返回多少行数据。因为优化器已经明确知道有多少个值（ `WHERE` 条件中的值）需要做索引查询，所以这里的表访问类型是 `const`。
第二步，MySQL 将第一步中返回的 `film_id` 列当做一个已知取值的列来处理。因为优化器清楚再第一步执行完成后，该值就会是明确的了。注意到正如第一步中一样，使用 `film_actor` 字段对表的访问类型也是 `const`。P212

如果 `ORDER BY` 子句中的所有列都来自关联的第一个表，那么 MySQL 在关联处理第一个表的时候就进行文件排序。如果是这样，那么在 MySQL 的 `EXPLAIN` 结果中可以看到 `Extra` 字段会有 `Using filesort`。除此之外的所有情况，MySQL 都会先将管理的结果存放到一个临时表中，然后在所有的关联都结束后，再进行文件排序。这时，在 MySQL 的 `EXPLAIN` 结果的 `Extra` 字段可以看到 `Using temporary; Using filesort`。`LIMIT` 会在排序后应用。P222

MySQL 5.6 当还需要返回部分查询结果时，不再对所有结果进行排序。

|      | 从这句话中也可以看出，如果可以，尽量使用一张表中的字段。 |
| ---- | -------------------------------------------------------- |

MySQL 5.0 之后的版本，在某些特殊的场景下是可以使用松散索引扫描的，例如，在一个分组查询中需要找到分组的最大值和最小值：

```mysql
EXPLAIN
SELECT
  actor_id,
  max(film_id)
FROM film_actor
GROUP BY actor_id \G

*************************** 1. row ***************************
           id: 1
  select_type: SIMPLE
        table: film_actor
   partitions: NULL
         type: range
possible_keys: PRIMARY,idx_fk_film_id
          key: PRIMARY
      key_len: 2
          ref: NULL
         rows: 201
     filtered: 100.00
        Extra: Using index for group-by
```

在 `EXPLAIN` 的 `Extra` 字段显示 “Using index for group-by”，表示这里将使用松散索引扫描。如果 MySQL 能写上 “loose index probe”，相信会更好理解。P231

## 附录 A: 参考资料

- [高性能MySQL](https://book.douban.com/subject/23008813/) — 很明显，这个文档就是这本书的读书笔记。😆
- [MySQL技术内幕](https://book.douban.com/subject/24708143/) — 和上面的那本参考看更爽。
- [数据库索引设计与优化](https://book.douban.com/subject/26419771/) — 第一本书引用了这本书，去年刚刚翻译过来，评分比第一本都高，9.4，惊为神作，不可不读。
- [如果有人问你数据库的原理，叫他看这篇文章](http://blog.jobbole.com/100349/)
- [MySQL索引背后的数据结构及算法原理](http://blog.codinglabs.org/articles/theory-of-mysql-index.html)
- [Treaps: CS 305 & 503 Lecture notes](http://bluehawk.monmouth.edu/rclayton/web-pages/s10-305-503/treaps.html)
- [演算法筆記 - Order](http://www.csie.ntnu.edu.tw/~u91029/Order.html)
- [硬盘内部硬件结构和工作原理详解](http://blog.csdn.net/tianxueer/article/details/2689117)
- [磁盘性能指标—IOPS 理论](http://elf8848.iteye.com/blog/1731274)
- [硬件性能解析(1)-存储金字塔](http://harrywu304.blog.163.com/blog/static/845660320101024111941414/)
- [深入理解计算机系统（1.3）---金字塔形的存储设备、操作系统的抽象概念](http://www.cnblogs.com/zuoxiaolong/p/computer3.html)
- [深入理解计算机系统 第 10 课 Memory Hierarchy](http://wdxtub.com/vault/csapp-10.html)
- [Magnetic Bubble Memories](http://www.daenotes.com/electronics/digital-electronics/magnetic-bubble-memories)
- [Operating Systems: Mass-Storage Structure](https://www.cs.uic.edu/~jbell/CourseNotes/OperatingSystems/10_MassStorage.html)
- [最受欢迎的 13 个数据库相关文章链接 - 编辑部的故事](https://my.oschina.net/editorial-story/blog/839446) — 还没有读，把这些文章读一读，吸收到这个笔记里来。
- [Mysql事务和隔离级别（read committed, repeatable read）_数据库_往事依稀浑似梦，都随风雨到心头！-CSDN博客](https://blog.csdn.net/sinat_27564919/article/details/70808991)
- [仅此一文让你明白事务隔离级别、脏读、不可重复读、幻读 - 李玉宝 - 博客园](https://www.cnblogs.com/yubaolee/p/10398633.html)
- [【mysql】Mysql的profile的使用 --- Profilling mysql的性能分析工具 - Angel挤一挤 - 博客园](https://www.cnblogs.com/sxdcgaq8080/p/11844079.html)
- [MySQL 有效利用 profile 分析 SQL 语句的执行过程 - 云+社区 - 腾讯云](https://cloud.tencent.com/developer/article/1449108)
- [MySQL EXPLAIN详解 - 简书](https://www.jianshu.com/p/ea3fc71fdc45)
- [MySQL - EXPLAIN详解 - 个人文章 - SegmentFault 思否](https://segmentfault.com/a/1190000012629884)
- [浅谈mysql fulltext全文索引优缺点 – 峰云就她了](http://xiaorui.cc/archives/2754)



