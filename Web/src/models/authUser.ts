export class User {
	name: string;
	sub: string;
	email: string;
	auth_time: number;
	token: string;

	constructor(name: string, sub: string, email: string, auth_time: number, token: string) {
		this.name = name;
		this.sub = sub;
		this.email = email;
		this.auth_time = auth_time;
		this.token = token;
	}
}
