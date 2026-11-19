import { existsSync } from "node:fs";
import { join } from "node:path";
import Gallery from "./gallery";

export default function Page() {
  return <Gallery downloadable={existsSync(join(process.cwd(), "public/widgets.apk"))} />;
}
