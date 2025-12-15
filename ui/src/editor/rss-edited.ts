import {
  type Editor,
  isActive,
  mergeAttributes,
  Node,
  nodeInputRule,
  type Range,
  VueNodeViewRenderer,
  type EditorState,
} from "@halo-dev/richtext-editor";
import RssView from "./RssView.vue";
import { markRaw } from "vue";
import { ToolboxItem } from "@halo-dev/richtext-editor";
import MdiShare from "~icons/mdi/share";
import BlockActionSeparator from "./BlockActionSeparator.vue";
import MdiDeleteForeverOutline from "~icons/mdi/delete-forever-outline?color=red";
import { deleteNode } from "../utils/delete-node";
import LayoutGrid from "@/icon/layout-grid.vue";
import LayoutList from "@/icon/layout-list.vue";
import FluentRss20Regular from '~icons/fluent/rss-20-regular';

declare module "@halo-dev/richtext-editor" {
  interface Commands<ReturnType> {
    "friends-rss": {
      setFriendsRss: (options: { src: string }) => ReturnType;
    };
  }
}

const FriendsRssExtension = Node.create({
  name: "friends-rss",
  fakeSelection: true,

  group() {
    return "block";
  },

  addAttributes() {
    return {
      ...this.parent?.(),
      src: {
        default: null,
        parseHTML: (element) => {
          return element.getAttribute("src");
        },
        renderHTML(element) {
          return { src: element.src };
        },
      },
      layout: {
        default: "list",
        parseHTML: (element) => {
          return element.getAttribute("layout");
        },
        renderHTML(element) {
          return { layout: element.layout };
        },
      },
    };
  },

  parseHTML() {
    return [
      {
        tag: "friends-rss",
      },
    ];
  },

  renderHTML({ HTMLAttributes }) {
    return ["friends-rss", mergeAttributes(HTMLAttributes)];
  },
  addCommands() {
    return {
      setFriendsRss:
        (options) =>
          ({ commands }) => {
            return commands.insertContent([
              {
                type: this.name,
                attrs: options,
              },
              { 
                type: "paragraph", 
                content: "" 
              },
            ]);
          },
    };
  },

  addInputRules() {
    return [
      nodeInputRule({
        find: /^\$friends-rss\$$/,
        type: this.type,
        getAttributes: (e) => ({ 
          src: e[1] 
        }),
      }),
    ];
  },

  addNodeView() {
    return VueNodeViewRenderer(RssView);
  },

  addOptions() {
    return {
      getCommandMenuItems() {
        return {
          priority: 2e2,
          icon: markRaw(FluentRss20Regular),
          title: "RSS展示",
          keywords: ["friends-rss", "rss", "RSS展示"],
          command: ({ editor, range }: { editor: Editor; range: Range }) => {
            editor
              .chain()
              .focus()
              .deleteRange(range)
              .setFriendsRss({ src: "" })
              .run();
          },
        };
      },
      getToolboxItems({ editor }: { editor: Editor }) {
        return {
          priority: 59,
          component: markRaw(ToolboxItem),
          props: {
            editor,
            icon: markRaw(FluentRss20Regular),
            title: "RSS展示",
            action: () => {
              editor
                .chain()
                .focus()
                .setFriendsRss({ src: "" })
                .run();
            },
          },
        };
      },
      getBubbleMenu({ editor }: { editor: Editor }) {
        return {
          pluginKey: "friends-rss-bubble-menu",
          shouldShow: ({ state }: { state: EditorState }) => {
            return isActive(state, FriendsRssExtension.name);
          },
          options: {
            placement: "top-start",
          },
          items: [
            {
              priority: 10,
              props: {
                icon: markRaw(LayoutList),
                title: "列表视图",
                isActive: () => editor.isActive(FriendsRssExtension.name,{ layout: "list" }),
                action: () => editor.commands.updateAttributes(FriendsRssExtension.name, { layout: "list",}),
              },
            },
            {
              priority: 20,
              props: {
                icon: markRaw(LayoutGrid),
                title: "网格试图",
                isActive: () => editor.isActive(FriendsRssExtension.name,{ layout: "grid" }),
                action: () => editor.commands.updateAttributes(FriendsRssExtension.name, { layout: "grid",}),
              },
            },
            {
              priority: 30,
              props: {
                icon: markRaw(MdiShare),
                title: "打开链接",
                action: () => {
                  window.open(editor.getAttributes(FriendsRssExtension.name).src, "_blank");
                },
              },
            },
            {
              priority: 40,
              component: markRaw(BlockActionSeparator),
            },
            {
              priority: 50,
              props: {
                icon: markRaw(MdiDeleteForeverOutline),
                title: "删除",
                action: ({ editor }: { editor: Editor }) => {
                  deleteNode(FriendsRssExtension.name, editor);
                },
              },
            },
          ],
        };
      },
    }
  }


})
export default FriendsRssExtension;
