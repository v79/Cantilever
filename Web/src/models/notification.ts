export interface Notification {
	message: string;
	shown: boolean;
	type: 'info' | 'warn' | 'error' | 'success';
}
