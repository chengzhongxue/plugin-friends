export interface RssDetail {
    'channel': Channel;
}

export interface Channel {

    'description'?: string;

    'items': Array<Item>;

    'link': string;

    'title': string;
}
export interface Item {

    'author'?: string;

    'description'?: string;

    'link': string;

    'pubDate': string;

    'title': string;

    'uri'?: string;
}