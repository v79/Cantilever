import type { ComponentType } from "svelte";

export type iconConfigType = {
    icon: ComponentType
    variation: 'filled' | 'outlined' | 'round' | 'two-tone' | 'sharp' | undefined;
    onClick: (e: Event) => {};
}