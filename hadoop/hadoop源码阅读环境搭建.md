


- [Mac搭建Hadoop源码阅读环境 - 骁枫 - 博客园](http://www.cnblogs.com/fillPv/p/6242239.html)



- protoc buffers编译器的下载【2.5.0版本】
    - 需要用这个编译器来编译生成hadoop使用的rpc协议
    - github地址： [https://github.com/protocolbuffers/protobuf/releases/tag/v2.5.0](https://github.com/protocolbuffers/protobuf/releases/tag/v2.5.0)

```
# linux操作步骤
wget https://github.com/protocolbuffers/protobuf/releases/download/v2.5.0/protobuf-2.5.0.tar.gz

tar -zxvf protobuf-2.5.0.tar.gz

cd protobuf-2.5.0

./configure
make
make check
make install
# 如果报没有权限的错误，su root 切换到root执行。
# 最后输入  protoc --version命令，如显示libprotoc 2.5.0则安装成功
```