import { definePlugin } from "@halo-dev/console-shared";
import Friend from "./views/Friend.vue";
import { IconLink } from "@halo-dev/components";
import { markRaw } from "vue";

export default definePlugin({
  name: "plugin-friends",
  components: {},
  routes: [
    {
      parentName: "Root",
      route: {
        path: "/friends",
        name: "Friends",
        component: Friend,
        meta: {
          title: "朋友圈",
          searchable: true,
          permissions: ["plugin:friends:view"],
          menu: {
            name: "朋友圈",
            group: "content",
            icon: markRaw(IconLink),
            priority: 21,
          },
        },
      },
    },
  ],
  extensionPoints: {},
});
