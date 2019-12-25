## Widge工作细节


点击执行的时候：

```
接口： http://localhost:11180/api/v3/views/{id}/getdata
关键参数项：
    - 维度，即groupby的字段
    - 指标：即聚合操作，有两个子参数，分别为 字段、聚合函数
    - 过滤：即where后面的内容。主要包含字段名/操作(in/like等)/值
    - 

{
	"groups": [
		"期限"
	],
	"aggregators": [
		{
			"column": "投资金额",
			"func": "sum"
		}
	],
	"filters": [
		{
			"name": "是否续购",
			"type": "filter",  # 为什么是这个类型
			"value": [
				"'已触发'"
			],
			"operator": "in", 
			"sqlType": "VARCHAR"
		}
	],
	"orders": [],
	"pageNo": 1,
	"pageSize": 20,
	"nativeQuery": false,
	"cache": false,
	"expired": 0,
	"flush": false
}
```



```
# sql
--------------------------------
select sum(投资金额) from ($view$) src
where 期限>=1 and 期限<=6
group by 产品类型，渠道
order by sum(投资金额) desc
---------------------------------
# 对应的json参数：
{
	"groups": [
		"产品类型",
		"渠道"
	],
	"aggregators": [
		{
			"column": "投资金额",
			"func": "sum"
		}
	],
	"filters": [
		{
			"type": "relation",
			"value": "and",
			"children": [
				{
					"name": "期限",
					"type": "filter",
					"value": 1,
					"operator": ">=",
					"sqlType": "DECIMAL"
				},
				{
					"name": "期限",
					"type": "filter",
					"value": 6,
					"operator": ">=",
					"sqlType": "DECIMAL"
				}
			]
		}
	],
	"orders": [
		{
			"column": "sum(投资金额)",
			"direction": "desc"
		}
	],
	"pageNo": 1,
	"pageSize": 20,
	"nativeQuery": false,
	"cache": false,
	"expired": 0,
	"flush": false
}
```