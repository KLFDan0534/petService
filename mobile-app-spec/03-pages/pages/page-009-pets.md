# page-009 · 宠物档案（Pets）

- **Purpose**：宠物建档与管理（下单前置数据）[E /pets /pets/:id]
- **Role**：登录（用户区）
- **Entry**：Tab「宠物」/ 下单流程；**Exit**：PetDetail 编辑 / 下单回填

## Layout

```
列表：2 列 PetCard（72px 圆头像(emoji/图) + 18px 衬线名 + meta 行）+ 新增卡 [E pet-card]
详情：头像 + 基本信息卡 + 健康备注 + 编辑入口
表单（PetFormDialog → 全屏页 [P]）：名字/物种/品种/性别/生日/体重/绝育/疫苗/照片(上传)/备注
```

## API（EXISTING PetController）

`GET /api/pets`（我的）· `GET /api/pets/{id}` · `POST/PUT/DELETE /api/pets` · 商家维度 `GET /api/pets/merchant` · 管理 `GET /api/pets/admin/all`。照片 `POST /api/files/upload`。

## States / 校验

名字/物种必填；体重数字；照片 ≤上限（upload 约束）；删除需确认（danger Dialog，软删除）。空态：引导建档（「添加第一只宠物」primary）。
归档数据被订单引用时删除由后端约束 → 错误 Snackbar 透传 `message`。
