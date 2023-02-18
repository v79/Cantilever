// AppStatus is a dumping ground for things which need to hang around

export interface AppStatus {
    activeFile: string,
    isNewFile: boolean,
    hasChanged: boolean,
    isValid: boolean,
    newSlug: string;
}