<template>
  <div :class="classObj" class="app-wrapper" :style="{ '--current-color': theme }">
    <div v-if="device === 'mobile' && sidebar.opened" class="drawer-bg" @click="handleClickOutside" />
    <side-bar v-if="showSidebar" class="sidebar-container" />
    <div :class="{ hasTagsView: needTagsView, sidebarHide: sidebar.hide }" class="main-container">
      <div :class="{ 'fixed-header': fixedHeader }" class="layout-header">
        <navbar @set-layout="setLayout" />
        <tags-view v-if="needTagsView" />
      </div>
      <app-main :class="{ 'with-fixed-header': fixedHeader, 'with-tags-view': needTagsView }" />
      <settings ref="settingRef" />
    </div>

    <!-- AI会话 浮动按钮（右下角，可拖放） -->
    <div
      class="ai-chat-float"
      :style="floatStyle"
      title="AI会话"
      @pointerdown="onFloatPointerDown"
      @pointermove="onFloatPointerMove"
      @pointerup="onFloatPointerUp"
      @pointercancel="onFloatPointerUp"
    >
      <img :src="aiChatIcon" alt="AI会话" draggable="false" />
    </div>

    <!-- AI 会话对话框（SSE 流式） -->
    <AiChatDialog v-model="aiChatVisible" />
  </div>
</template>

<script setup lang="ts">
import { NavTypeEnum } from '@/enums/NavTypeEnum';
import { useAppStore } from '@/store/modules/app';
import { useSettingsStore } from '@/store/modules/settings';
import { initMessageBox, initPush } from '@/utils/push';
import aiChatIcon from '@/assets/images/ai-chat-float.png';
import { AppMain, Navbar, Settings, TagsView } from './components';
import SideBar from './components/Sidebar/index.vue';
import AiChatDialog from '@/components/AiChatDialog/index.vue';

const settingsStore = useSettingsStore();
const theme = computed(() => settingsStore.theme);
const sidebar = computed(() => useAppStore().sidebar);
const device = computed(() => useAppStore().device);
const needTagsView = computed(() => settingsStore.tagsView);
const fixedHeader = computed(() => settingsStore.fixedHeader);
const layout = computed(() => settingsStore.navType);

// 根据布局模式判断是否显示侧边栏
const showSidebar = computed(() => {
  if (sidebar.value.hide) return false;
  return layout.value === NavTypeEnum.LEFT || layout.value === NavTypeEnum.MIX;
});

const classObj = computed(() => ({
  hideSidebar: !sidebar.value.opened,
  openSidebar: sidebar.value.opened,
  withoutAnimation: sidebar.value.withoutAnimation,
  mobile: device.value === 'mobile'
}));

const { width } = useWindowSize();
const WIDTH = 992; // refer to Bootstrap's responsive design

watch(
  width,
  w => {
    if (w - 1 < WIDTH) {
      useAppStore().toggleDevice('mobile');
      useAppStore().closeSideBar({ withoutAnimation: true });
    } else {
      useAppStore().toggleDevice('desktop');
    }
  },
  { immediate: true }
);

const settingRef = ref<InstanceType<typeof Settings>>();

onMounted(async () => {
  try {
    await initMessageBox();
  } finally {
    initPush();
  }
});

const handleClickOutside = () => {
  useAppStore().closeSideBar({ withoutAnimation: false });
};

const setLayout = () => {
  settingRef.value?.openSetting();
};

const aiChatVisible = ref(false);

// ---- AI会话 浮动按钮：右下角默认定位，支持任意拖放 ----
const floatLeft = ref<number | null>(null);
const floatTop = ref<number | null>(null);
const floatDrag = reactive({ active: false, moved: false, startX: 0, startY: 0, origLeft: 0, origTop: 0 });

const floatStyle = computed(() => {
  if (floatLeft.value == null || floatTop.value == null) {
    return { right: '18px', bottom: '18px' };
  }
  return { left: `${floatLeft.value}px`, top: `${floatTop.value}px`, right: 'auto', bottom: 'auto' };
});

// 全局缩放（html { zoom: --app-scale }）下，视觉像素与布局像素相差 --app-scale 倍
const getAppScale = (): number => {
  return Number.parseFloat(getComputedStyle(document.documentElement).getPropertyValue('--app-scale')) || 1;
};

const onFloatPointerDown = (e: PointerEvent) => {
  const el = e.currentTarget as HTMLElement;
  const r = el.getBoundingClientRect();
  const scale = getAppScale();
  floatDrag.active = true;
  floatDrag.moved = false;
  floatDrag.startX = e.clientX;
  floatDrag.startY = e.clientY;
  // getBoundingClientRect / clientX 是视觉像素，left/top 样式在 zoom 子树里按布局像素渲染，需换算
  floatDrag.origLeft = r.left / scale;
  floatDrag.origTop = r.top / scale;
  floatLeft.value = r.left / scale;
  floatTop.value = r.top / scale;
  el.setPointerCapture(e.pointerId);
};

const onFloatPointerMove = (e: PointerEvent) => {
  if (!floatDrag.active) return;
  const scale = getAppScale();
  const dx = (e.clientX - floatDrag.startX) / scale;
  const dy = (e.clientY - floatDrag.startY) / scale;
  if (Math.abs(dx) + Math.abs(dy) > 4) {
    floatDrag.moved = true;
  }
  const size = 58;
  // 视口尺寸是视觉像素，定位坐标写在 zoom 子树里按布局像素渲染，需按缩放换算
  const vw = window.innerWidth / scale;
  const vh = window.innerHeight / scale;
  floatLeft.value = Math.max(-size + 24, Math.min(vw - 24, floatDrag.origLeft + dx));
  floatTop.value = Math.max(8, Math.min(vh - 24, floatDrag.origTop + dy));
};

const onFloatPointerUp = (e: PointerEvent) => {
  if (!floatDrag.active) return;
  floatDrag.active = false;
  const el = e.currentTarget as HTMLElement;
  try {
    el.releasePointerCapture(e.pointerId);
  } catch {
    /* ignore */
  }
  // 未发生拖拽视为点击，打开 AI 会话对话框
  if (!floatDrag.moved) {
    aiChatVisible.value = true;
  }
};
</script>

<style lang="scss" scoped>
@use '@/assets/styles/mixin.scss';
@use '@/assets/styles/tokens/sass-vars' as *;

.app-wrapper {
  @include mixin.clearfix;
  position: relative;
  height: 100%;
  width: 100%;
  background: var(--app-shell-bg);

  &.mobile.openSidebar {
    position: fixed;
    top: 0;
  }
}

.drawer-bg {
  background: #000;
  opacity: 0.4;
  width: 100%;
  top: 0;
  height: 100%;
  position: absolute;
  z-index: 999;
}

.layout-header {
  position: relative;
  z-index: 9;
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 12px 12px 0;
  background: transparent;
}

.fixed-header {
  position: fixed;
  top: 0;
  right: 0;
  width: calc(100% - #{$base-sidebar-width} - 12px);
  transition: width 0.28s;
}

.hideSidebar .fixed-header {
  width: calc(100% - 70px);
}

.sidebarHide .fixed-header {
  width: 100%;
}

.mobile .fixed-header {
  width: 100%;
  top: 0;
}

.ai-chat-float {
  position: fixed;
  z-index: 2000;
  width: 58px;
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  user-select: none;
  touch-action: none;
  -webkit-user-select: none;

  img {
    display: block;
    width: 92%;
    height: 92%;
    object-fit: contain;
    // 只对彩色圆本身投影（透明部分不产生阴影），无白底
    filter: drop-shadow(0 4px 10px rgba(0, 0, 0, 0.22));
    pointer-events: none;
  }

  &:active {
    cursor: grabbing;
  }
}
</style>
