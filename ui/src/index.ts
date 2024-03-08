import { definePlugin } from "@halo-dev/console-shared";
import Friend from "./views/Friend.vue";
import RiBloggerLine from '~icons/ri/blogger-line';
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
            icon: markRaw(RiBloggerLine),
            priority: 21,
          },
        },
      },
    },
  ],
  extensionPoints: {},
});
