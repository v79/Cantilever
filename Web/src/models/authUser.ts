export class User {
    name: String;
    sub: String;
    email: String;
    auth_time: Number;
    token: String;

    constructor(name: String, sub: String, email: String, auth_time: Number, token: String) {
        this.name = name;
        this.sub = sub;
        this.email = email;
        this.auth_time = auth_time;
        this.token = token;
    }
}
