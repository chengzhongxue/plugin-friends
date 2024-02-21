import apiClient from "@/utils/api-client";
import {Dialog, Toast} from "@halo-dev/components";

export const synchronizationRss = () => {
  Dialog.warning({
    title: "同步RSS数据",
    description: "点击按钮后，后台将进行同步RSS数据。",
    confirmType: "danger",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        await apiClient.post("/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-friends/friendPost/synchronizationFriend")
          .then((res: any) => {
            Toast.success("同步RSS数据成功");
          });
      } catch (e) {
        console.error("", e);
      }
    },
  });
}
