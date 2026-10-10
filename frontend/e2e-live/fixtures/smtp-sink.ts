import { createServer, type Server, type Socket } from "node:net";

export class SmtpSink {
  private readonly messages: string[] = [];
  private readonly waiters: Array<(message: string) => void> = [];
  private server: Server | undefined;

  async start(port = 1025) {
    this.server = createServer((socket) => this.accept(socket));
    await new Promise<void>((resolve, reject) => {
      this.server?.once("error", reject);
      this.server?.listen(port, resolve);
    });
  }

  async stop() {
    if (!this.server) return;
    await new Promise<void>((resolve, reject) => {
      this.server?.close((error) => (error ? reject(error) : resolve()));
    });
  }

  nextMessage(timeoutMs = 15_000): Promise<string> {
    const queued = this.messages.shift();
    if (queued !== undefined) return Promise.resolve(queued);

    return new Promise<string>((resolve, reject) => {
      const timeout = setTimeout(
        () => reject(new Error("Timed out waiting for SMTP delivery")),
        timeoutMs,
      );
      this.waiters.push((message) => {
        clearTimeout(timeout);
        resolve(message);
      });
    });
  }

  private accept(socket: Socket) {
    let buffer = "";
    let receivingData = false;
    let messageLines: string[] = [];
    socket.setEncoding("utf8");
    socket.write("220 localhost TinyRoute E2E SMTP\r\n");
    socket.on("data", (chunk) => {
      buffer += chunk;
      let lineEnd = buffer.indexOf("\r\n");
      while (lineEnd >= 0) {
        const line = buffer.slice(0, lineEnd);
        buffer = buffer.slice(lineEnd + 2);
        if (receivingData) {
          if (line === ".") {
            receivingData = false;
            this.deliver(messageLines.join("\n"));
            messageLines = [];
            socket.write("250 2.0.0 queued\r\n");
          } else {
            messageLines.push(line.startsWith("..") ? line.slice(1) : line);
          }
        } else if (/^(EHLO|HELO)\b/i.test(line)) {
          socket.write("250-localhost\r\n250 8BITMIME\r\n");
        } else if (/^(MAIL FROM|RCPT TO|RSET)\b/i.test(line)) {
          socket.write("250 2.1.0 ok\r\n");
        } else if (/^DATA\b/i.test(line)) {
          receivingData = true;
          socket.write("354 End data with <CR><LF>.<CR><LF>\r\n");
        } else if (/^QUIT\b/i.test(line)) {
          socket.end("221 2.0.0 bye\r\n");
        } else {
          socket.write("250 2.0.0 ok\r\n");
        }
        lineEnd = buffer.indexOf("\r\n");
      }
    });
  }

  private deliver(message: string) {
    const waiter = this.waiters.shift();
    if (waiter) waiter(message);
    else this.messages.push(message);
  }
}
