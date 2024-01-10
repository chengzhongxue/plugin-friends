import { definePlugin } from "@halo-dev/console-shared";
import Friend from "./views/Friend.vue";
import { IconPlug } from "@halo-dev/components";
import { markRaw } from "vue";

export default definePlugin({
  name: "PluginFriends",
  components: {},
  routes: [
    {
      parentName: "Root",
      route: {
        path: "/friends",
        name: "Friends",
        component: Friend,
        meta: {
          title: "RSS订阅",
          searchable: true,
          permissions: ["plugin:friends:view"],
          menu: {
            name: "RSS订阅",
            group: "content",
            icon: markRaw(IconPlug),
            priority: 0,
          },
        },
      },
    },
  ],
  extensionPoints: {},
});
