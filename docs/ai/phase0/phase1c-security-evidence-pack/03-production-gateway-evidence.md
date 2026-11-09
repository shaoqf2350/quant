# Production Gateway /api/quant 证据包

状态：`TEMPLATE_AND_LOCAL_EVIDENCE_ONLY`
生产证据：`UNKNOWN`

## 当前仓库示例

文件：`script/docker/nginx/conf/nginx.conf`

```nginx
location /prod-api/ {
    proxy_pass http://server/;
}
```

这只是仓库示例，不是生产 `/api/quant` 证据。

## 生产待补字段

- 环境和网关产品/版本；
- 配置版本/变更单号；
- 外部健康路径；
- 外部 Paper API 路径；
- `/api/quant` 是否原样转发；
- 是否追加/剥离 `/api`；
- rewrite/重复前缀规则；
- 受控 HTTP 验证编号、时间、验证人；
- 脱敏 HTTP status/body.code。

## 当前结论

```text
repository_sample_route: /prod-api/ -> backend root
production_/api/quant_route: UNKNOWN
```
