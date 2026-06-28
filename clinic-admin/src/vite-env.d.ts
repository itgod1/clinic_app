/// <reference types="vite/client" />

declare module 'element-plus/dist/locale/zh-cn.mjs' {
  const zhCn: Record<string, unknown>
  export default zhCn
}

declare module 'element-plus' {
  const ElementPlus: Record<string, unknown>
  export default ElementPlus
}

declare global {
  namespace JSX {
    interface IntrinsicElements {
      [elemName: string]: any
    }
  }
}

export {}
