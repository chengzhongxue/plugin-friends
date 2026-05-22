<script lang="ts" setup>
import {useAttrs} from "vue";
import {axiosInstance} from "@halo-dev/api-client";
import type {Link} from "@/api/generated";

const handleSelectRemote = {
  search: async ({ keyword, page, size }: { keyword: string; page: number; size: number }) => {
    const { data:links } = await axiosInstance.get("/apis/api.link.halo.run/v1alpha1/links",{
      params:{
        page: page,
        size: size,
        keyword: keyword,
      }
    });
    return {
      options: links.items?.map((item:Link) => ({
        label: item.spec?.displayName,
        value: item.metadata.name,
      })),
      total: links.total,
      page: links.page,
      size: links.size,
    };
  },
  findOptionsByValues: async (names: any) => {
    if (names.length === 0) {
      return [];
    }
    const { data: findLinks } = await axiosInstance.get("/apis/api.link.halo.run/v1alpha1/links",{
      params:{
        fieldSelector: [`metadata.name=(${names.join(",")})`]
      }
    });
    return findLinks.items?.map((item:Link) => ({
      value: item.metadata.name,
      label: item.spec?.displayName
    }))
  },
};

const attrs = useAttrs();

</script>
<template>
  <FormKit
    v-bind="attrs"
    type="select"
    searchable
    remote
    multiple
    :remote-option="handleSelectRemote"
  ></FormKit>
</template>
