# 云护宠 PetCare OS 前台介绍页

这是一个独立静态展示页，用于毕业设计前台介绍、答辩演示或作品集展示。它没有改动主项目任何现有文件，全部内容位于 `pet-care-showcase/`。

## 打开方式

直接用浏览器打开：

```text
D:\code\ideaProject\petService\pet-care-showcase\index.html
```

## 使用的设计依据

- UI UX Pro Max：叙事式滚动、Bento Grid、宠物科技配色、动画与可访问性检查。
- 项目现有 `PRODUCT.md`：实用、可信、温暖，围绕宠物寄养和服务管理业务。
- 当前项目模块：用户、宠物、商家、看护人、服务、订单、支付、退款、客服、投诉、会员、AI、统计和后台审核。
- 在线参考：
  - https://github.com/nextlevelbuilder/ui-ux-pro-max-skill
  - https://colorlib.com/wp/pet-care-websites/

## 设计师清单

- 首屏直接展示“云护宠 PetCare OS”，让项目主题在第一屏成立。
- 使用橙色表达亲和，蓝色表达信任，绿色表达服务完成状态。
- 用 Bento 模块组织复杂能力，避免长篇堆文字。
- 使用流程叙事解释一次寄养从预约到评价的业务链路。
- 增加“角色体验台”，可在宠物主人、商家端、管理端之间切换，方便答辩时讲多角色协同。
- 增加预约演示弹层，让前台页不只是展示，还具备一个完整的轻交互入口。
- 保持 8px 以内圆角，整体偏产品介绍页，不做过度营销化装饰。

## 工程师清单

- 独立静态 HTML/CSS/JS，无构建依赖。
- 响应式适配桌面、平板和手机。
- 支持键盘焦点、跳转主内容、ARIA 导航标签。
- 支持 `prefers-reduced-motion`，降低动态偏好下停止复杂动画。
- 动画只使用 transform、opacity 和背景更新，避免布局抖动。
- 交互包括滚动进度、滚动入场、计数动画、移动端导航、卡片聚光、角色标签页、`dialog` 弹层和 `aria-live` 提示。
- 锚点跳转保留顶部空间，避免粘性导航遮挡标题。

## 验收截图

- `qa-desktop.png`：桌面首屏。
- `qa-tablet.png`：平板首屏。
- `qa-mobile.png`：手机首屏。
- `qa-desktop-role-demo.png`：桌面角色体验区。
- `qa-mobile-role-demo.png`：手机角色体验区。
- `qa-reduced-motion.png`：低动态偏好下的手机首屏。
