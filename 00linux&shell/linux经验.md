## linux经验

Linux环境变量
1）修改/etc/profile文件：用来设置系统环境参数，比如$PATH. 这里面的环境变量是对系统内所有用户生效。使用bash命令，需要source  /etc/profile一下。
2）修改~/.bashrc文件：针对某一个特定的用户，环境变量的设置只对该用户自己有效。使用bash命令，只要以该用户身份运行命令行就会读取该文件。
3）把/etc/profile里面的环境变量追加到~/.bashrc目录
```
[appuser@hadoop102 ~]$ cat /etc/profile >> ~/.bashrc
[appuser@hadoop103 ~]$ cat /etc/profile >> ~/.bashrc
[appuser@hadoop104 ~]$ cat /etc/profile >> ~/.bashrc
```
4）说明
登录式Shell，采用用户名比如appuser登录，会自动加载/etc/profile
非登录式Shell，采用ssh 比如ssh hadoop103登录，不会自动加载/etc/profile，会自动加载~/.bashrc
