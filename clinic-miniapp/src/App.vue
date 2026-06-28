<script setup>
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'

onLaunch(() => {
  console.log('App Launch')
  // 不再强制检查登录，让用户可以先浏览首页
})

onShow(() => {
  console.log('App Show')
})

onHide(() => {
  console.log('App Hide')
})
</script>

<style lang="scss">
/* 全局样式 */
page {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 重置样式 */
view,
text,
input,
textarea,
button,
scroll-view {
  box-sizing: border-box;
}

/* 隐藏滚动条 */
::-webkit-scrollbar {
  display: none;
  width: 0 !important;
  height: 0 !important;
  -webkit-appearance: none;
  background: transparent;
}

/* 按钮重置 */
button {
  margin: 0;
  padding: 0;
  background: none;
  border: none;
  line-height: inherit;

  &::after {
    border: none;
  }
}

/* 输入框重置 */
input,
textarea {
  outline: none;
  border: none;
  background: none;
}

/* 图片重置 */
image {
  display: block;
  max-width: 100%;
}

/* 安全区域适配 */
.safe-area-bottom {
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
}
</style>
