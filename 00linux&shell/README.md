# shell-linux
linux与shell相关知识



```
#!/bin/bash
```
"#!"告诉系统执行整个脚本的时候要使用 /bin/bash 这个解释器。但是，如果使用
```
bash xxx.sh 
```
这种方式来运行shell脚本时，那么"#!/bin/bash"就可以省略不写。

运行shell的两种方式：
- 方式1： bash xxx.sh 这种方式xxx.sh是普通文件即可
- 方式2： ./xxx.sh  这种方式要求xxx.sh 需要有可执行权限，否则执行的时候会报错
```
chmod +x ./xxx.sh
```
第2种方式的衍生用法是将xxx.sh设置为系统命令，那么只需要将xxx.sh 放到 $PATH 变量所包含的目录下即可。

> 为了更清晰地看到脚本运行的过程，还可以借助-x参数来观察脚本的运行情况

点号用于执行某个脚本，甚至脚本没有可执行权限也可以运行
```
. ./xxx.sh 
```
与点号类似，source命令也可读取并在当前环境中执行脚本，同时还可返回脚本中最后一个命令的返回状态；如果没有返回值则返回0，代表执行成功

声明变量：declare、typeset

从一个循环（for、while、until或者select）中退出。break后可以跟一个数字n，代表跳出n层循环，n必须大于1


eval
- 用法1：将字符串解析成命令执行
- 用法2：程序运行中根据某个变量确定实际的变量名
```
[appuser@hadoop101 linuxshell]$ name1=66
[appuser@hadoop101 linuxshell]$ no=1
[appuser@hadoop101 linuxshell]$ eval echo "$"name$no
66
```
- 用法3：将一个变量的值作为另一个变量名，并赋值
```
[appuser@hadoop101 linuxshell]$ name=result
[appuser@hadoop101 linuxshell]$ value=bigdata
[appuser@hadoop101 linuxshell]$ eval $name=$value
[appuser@hadoop101 linuxshell]$ echo $result
bigdata

# 加上export命令，就会变为环境变量
[appuser@hadoop101 linuxshell]$ eval export $name="$value"666
[appuser@hadoop101 linuxshell]$ echo $result
bigdata666
[appuser@hadoop101 linuxshell]$ env | grep result
result=bigdata666

```

exec：执行命令来取代当前的Shell
> 内建命令exec并不启动新的Shell，而是用要被执行的命令替换当前的Shell进程，并且将老进程的环境清理掉，而且exec命令后的其他命令将不再执行
- 一般放在shell文件中使用
- exec典型的用法是与find联合使用，用find找出符合匹配的文件，然后交给exec处理
```
# 列出/usr/loacl/hbase目录下以site结尾的xml文件
[appuser@hadoop101 linuxshell]$ find /usr/local/hbase/ -name "*site.xml" -exec ls -l {} \;
-rw-r--r-- 1 appuser appuser 1614 May 14 21:31 /usr/local/hbase/conf/hbase-site.xml
lrwxrwxrwx 1 appuser appuser 42 May 14 09:40 /usr/local/hbase/conf/core-site.xml -> /usr/local/hadoop/etc/hadoop/core-site.xml
lrwxrwxrwx 1 appuser appuser 42 May 14 09:42 /usr/local/hbase/conf/hdfs-site.xml -> /usr/local/hadoop/etc/hadoop/hdfs-site.xml
# 删除tmp临时文件
[appuser@hadoop101 linuxshell]$ find /usr/local/hbase/ -name "*.tmp" -exec rm -f {} \;
# 使用$?可以取出上个命令退出的状态
[appuser@hadoop101 linuxshell]$ echo $?
0

```
13.退出Shell：exit
> 该命令可以接受的参数是一个状态值n，代表退出的状态。默认状态值是0

14.使变量能被子Shell识别：export
> 要说明的是，即便子Shell确实读取到了父Shell中变量var的值，也只是值的传递，如果在子Shell中尝试改变var的值，改变的只是var在子Shell中的值，父Shell中的该值并不会因此受到影响

15.发送信号给指定PID或进程：kill

Linux操作系统包括3种不同类型的进程
- 交互进程，由一个Shell启动的进程，既可以在前台运行，也可以在后台运行
- 批处理进程，与终端没有联系，是一个进程序列
- 监控进程，也成守护进程

16.整数运算：let
- 用于整数运算
- 也支持类C的计算方式
```
[appuser@hadoop101 linuxshell]$ i=100
[appuser@hadoop101 linuxshell]$ let i-=20
[appuser@hadoop101 linuxshell]$ echo $i
80
[appuser@hadoop101 linuxshell]$ let j=i/6   # 取除法的整数
[appuser@hadoop101 linuxshell]$ echo $j
13
[appuser@hadoop101 linuxshell]$ let r=j**2  # 平方
[appuser@hadoop101 linuxshell]$ echo $r
169
[appuser@hadoop101 linuxshell]$ let y=i%6   # 求余数
[appuser@hadoop101 linuxshell]$ echo $y
2
```
17.显示当前工作目录：pwd
> -P选项，打印出的路径名中不会包含符号连接（可以显示真实路径，对于软链接路径）
> -L选项，打印出的路径中可以包含符号连接

18.声明局部变量：local

19.从标准输入读取一行到变量：read
```
[appuser@hadoop101 linuxshell]$ cat test_read.sh 
#!/bin/bash

echo -n "用法1 请输入内容："
read N
echo "你输入了：" $N


read -p "用法2 请输入内容：" M
echo "你输入了：" $M
```
执行的结果：
```
[appuser@hadoop101 linuxshell]$ bash test_read.sh 
用法1 请输入内容：big
你输入了： big
用法2 请输入内容：data
你输入了： data
```
如果不指定变量，read命令会将读取到的值放入环境变量REPLY中
> 注意read是按行读的
```
[appuser@hadoop101 linuxshell]$ read
good day day up
[appuser@hadoop101 linuxshell]$ echo $REPLY
good day day up
```

20.定义函数返回值：return
> 常见用法是return n，其中n是一个指定的数字，使函数以指定值退出。如果没有指定n值，则返回状态是函数体中执行的最后一个命令的退出状态

21.向左移动位置参数：shift

“位置参数”:假设一个脚本在运行时可以接受参数，那么
- 从左到右第一个参数被记作$1，第二个参数为$2，以此类推，第n个参数为$N。
- 所有参数记作$@或$*，参数的总个数记作$#，
- 脚本本身记作$0

> 假设脚本有A、B、C这3个参数，那么$1为A，$2为B，$3为C；shift一次后，$1为B，$2为C；再次shift后$1为C

22.显示并设置进程资源限度：ulimit

Linux系统的各个资源都做了软硬限制，其中硬限制的作用是控制软限制（换言之，软限制不能高于硬限制）。使用ulimit-a可以查看当前系统的软限制（使用命令ulimit-a-H可查看系统的硬限制）

```
# -n代表打开的文件数。下面的命令设置打开的最大文件数未4096，会同时设置硬限制和软限制
ulimit -n 4096
# 只设置软限制
ulimit -n 4096
# 查看设置的结果
umlimit -n
```
使用ulimit直接调整参数，重启系统失效，可以修改到/etc/security/limits.conf中永久生效

23.测试表达式：test