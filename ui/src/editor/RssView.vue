<script lang="ts" setup>
import type {NodeViewProps} from "@halo-dev/richtext-editor";
import { NodeViewWrapper } from "@halo-dev/richtext-editor";
import { computed, onMounted, ref, watch } from "vue";
import { VButton,VSpace,VDropdown,VEmpty} from "@halo-dev/components";
import { friendsApiClient } from "@/api";
import type { RssDetail } from "@/api/generated";
import type { FriendsRss } from '@kunkunyu/friends-rss';

const selecteRssDetail = ref<RssDetail | undefined>();

const props = defineProps<NodeViewProps>();

const rssRef = ref<InstanceType<typeof FriendsRss> | null>();

watch(
  () => props.node.attrs.src,
  (value) => {
    if (value && rssRef.value) {
      rssRef.value.src = value;
      rssRef.value.fetchRssDetail();
    }
  }
);

const src = computed({
  get: () => {
    return props.node?.attrs.src;
  },
  set: (src: string) => {
    props.updateAttributes({ src: src });
  },
});

const layout = computed({
  get: () => {
    return props.node?.attrs.layout;
  },
  set: (layout: string) => {
    props.updateAttributes({ layout: layout });
  },
});


const editorLinkObtain = ref();

onMounted(() => {
  if (!src.value) {
  }else {
    handleCheckAllChange();
  }
});

const handleCheckAllChange = async () => {
  const { data: data } = await friendsApiClient.friendPost.parsingRss({
    rssUrl: src.value
  });
  selecteRssDetail.value = data
};

const handleEnterSetExternalLink = () => {
  if (!editorLinkObtain.value) {
    return;
  }
  props.updateAttributes({
    src: editorLinkObtain.value,
  });
};

const handleResetInit = () => {
  props.updateAttributes({src: ""});
};

</script>

<template>
  <node-view-wrapper as="div" class="contact-friends-rss-container"
      :class="{
        'contact-friends-rss-container--selected': selected,
      }">
    <div class="contact-friends-rss-nav">
      <div class="contact-friends-rss-nav-start">
        <div>RSS</div>
      </div>
      <div class="contact-friends-rss-nav-end">
        <button v-if="src && selecteRssDetail?.channel!=undefined"
                @click="handleResetInit"
                class=":uno: btn-sm btn-default btn" type="button">
          <span class=":uno: btn-content">更换</span>
        </button>
      </div>
    </div>
    <div class="contact-friends-rss-preview" >
      <VEmpty message="当前未输入RSS链接，点击下方按钮链接" title="未输入RSS链接"
              v-if="!src || selecteRssDetail?.channel==undefined">
        <template #actions>
          <VSpace>
            <VDropdown>
              <VButton>输入RSS链接</VButton>
              <template #popper>
                <input
                  v-model="editorLinkObtain"
                  class=":uno: block w-full rounded-md border border-gray-300 bg-gray-50 px-2 py-1.5 text-sm text-gray-900 hover:bg-gray-100"
                  placeholder="输入链接，按回车确定"
                  @keydown.enter="handleEnterSetExternalLink"
                  @change="handleCheckAllChange"
                />
              </template>
            </VDropdown>
          </VSpace>
        </template>
      </VEmpty>
      <div v-else inert="true" >
        <friends-rss
          ref="rssRef"
          :src="src"
          :layout="layout"
        ></friends-rss>
      </div>
    </div>
  </node-view-wrapper>
</template>

