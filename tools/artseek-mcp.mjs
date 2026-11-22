#!/usr/bin/env node
// Minimal stdio MCP server exposing the pinned artseek CLI to coding agents.
// POSIX only: tools/artseek.sh drives adb and the rooted device.
// Protocol: newline-delimited JSON-RPC 2.0 on stdin/stdout; logs belong on stderr.
import { spawn } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createInterface } from 'node:readline';

const here = dirname(fileURLToPath(import.meta.url));
const project = resolve(here, '..');
const wrapper = resolve(here, 'artseek.sh');
const timeoutMs = Number(process.env.ARTSEEK_TIMEOUT_MS || 180_000);

const TOOLS = [
    {
        name: 'artseek_ping',
        description:
            'Check that the pinned artseek binary, the on-device ART layout cache and the target process are usable. Reads no heap objects.',
        inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    },
    {
        name: 'artseek_roots',
        description:
            'List live Activity and ViewRootImpl instances in the running app with window bounds and focus state.',
        inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    },
    {
        name: 'artseek_tree',
        description:
            'Dump the Android View hierarchy. Compose content appears only as AndroidComposeView nodes; use artseek_strsearch for Compose text.',
        inputSchema: {
            type: 'object',
            properties: {
                rootAddr: { type: 'string', description: 'ViewRootImpl address from artseek_roots, e.g. 0xa3cc018' },
                maxDepth: { type: 'integer', minimum: 1 },
                focused: { type: 'boolean', description: 'Only the focused window' },
            },
            additionalProperties: false,
        },
    },
    {
        name: 'artseek_strsearch',
        description:
            'Search live java.lang.String instances and list the objects that reference them. Works for Compose text nodes.',
        inputSchema: {
            type: 'object',
            properties: {
                needle: { type: 'string', description: 'Literal substring to find' },
                limit: { type: 'integer', minimum: 1 },
                preview: { type: 'boolean', description: 'Include a field preview of each owner object' },
            },
            required: ['needle'],
            additionalProperties: false,
        },
    },
    {
        name: 'artseek_instances',
        description:
            'List live instances of a class. Accepts a dot name, a JNI descriptor, or a glob such as *Course.',
        inputSchema: {
            type: 'object',
            properties: {
                type: { type: 'string', description: 'Class name, JNI descriptor or glob' },
                limit: { type: 'integer', minimum: 1 },
            },
            required: ['type'],
            additionalProperties: false,
        },
    },
    {
        name: 'artseek_dumpobj',
        description: 'Dump the fields of one object by address.',
        inputSchema: {
            type: 'object',
            properties: {
                addr: { type: 'string', description: 'Object address, e.g. 0xa3c9dd0' },
                depth: { type: 'integer', minimum: 0 },
                schemaOnly: { type: 'boolean', description: 'Dump the field schema only' },
            },
            required: ['addr'],
            additionalProperties: false,
        },
    },
    {
        name: 'artseek_whorefs',
        description: 'List live objects that reference the given object address.',
        inputSchema: {
            type: 'object',
            properties: {
                addr: { type: 'string', description: 'Target object address, e.g. 0xa3abe40' },
                limit: { type: 'integer', minimum: 1 },
            },
            required: ['addr'],
            additionalProperties: false,
        },
    },
    {
        name: 'artseek_query',
        description:
            'Run one artseek QuerySpec, e.g. {"root":{"kind":"scan","type":"*Course"},"steps":[{"op":"ref"}],"collection":true}. See protocol/protocol.md in the artseek repository for the grammar.',
        inputSchema: {
            type: 'object',
            properties: {
                spec: { type: 'object', description: 'QuerySpec JSON object' },
            },
            required: ['spec'],
            additionalProperties: false,
        },
    },
    {
        name: 'artseek_selfcheck',
        description: 'Validate the ART layout against the running process without recalibrating.',
        inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    },
    {
        name: 'artseek_calibrate',
        description:
            'Force sentinel calibration and rewrite /data/local/tmp/.artseek/layout.json. Use after an ART update when other calls report calibration failures.',
        inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    },
];

function run(args) {
    return new Promise((done) => {
        let settled = false;
        const finish = (result) => {
            if (settled) return;
            settled = true;
            clearTimeout(timer);
            done(result);
        };

        const child = spawn('bash', [wrapper, ...args], {
            cwd: project,
            stdio: ['ignore', 'pipe', 'pipe'],
        });
        let stdout = '';
        let stderr = '';
        const timer = setTimeout(() => {
            stderr += `\nartseek: timed out after ${timeoutMs}ms`;
            child.kill('SIGKILL');
        }, timeoutMs);

        child.stdout.setEncoding('utf8');
        child.stderr.setEncoding('utf8');
        child.stdout.on('data', (chunk) => { stdout += chunk; });
        child.stderr.on('data', (chunk) => { stderr += chunk; });
        child.on('error', (error) => finish({ code: -1, stdout, stderr: `${stderr}${error.message}` }));
        child.on('close', (code) => finish({ code, stdout, stderr }));
    });
}

function fail(message) {
    return { content: [{ type: 'text', text: message }], isError: true };
}

async function callTool(name, args) {
    const argv = [];
    switch (name) {
        case 'artseek_ping':
            argv.push('ping');
            break;
        case 'artseek_roots':
            argv.push('roots');
            break;
        case 'artseek_tree':
            argv.push('tree');
            if (args.rootAddr) argv.push('--root-addr', args.rootAddr);
            if (args.maxDepth !== undefined) argv.push('--max-depth', String(args.maxDepth));
            if (args.focused) argv.push('--focused');
            break;
        case 'artseek_strsearch':
            argv.push('strsearch', args.needle);
            if (args.limit !== undefined) argv.push('--limit', String(args.limit));
            if (args.preview) argv.push('--preview');
            break;
        case 'artseek_instances':
            argv.push('instances', args.type);
            if (args.limit !== undefined) argv.push('--limit', String(args.limit));
            break;
        case 'artseek_dumpobj':
            argv.push('dumpobj', args.addr);
            if (args.depth !== undefined) argv.push('--depth', String(args.depth));
            if (args.schemaOnly) argv.push('--schema-only');
            break;
        case 'artseek_whorefs':
            argv.push('whorefs', args.addr);
            if (args.limit !== undefined) argv.push('--limit', String(args.limit));
            break;
        case 'artseek_query':
            argv.push('query', JSON.stringify(args.spec));
            break;
        case 'artseek_selfcheck':
            argv.push('selfcheck');
            break;
        case 'artseek_calibrate':
            argv.push('calibrate');
            break;
        default:
            throw new Error(`unknown tool: ${name}`);
    }

    const result = await run(argv);
    if (result.code !== 0) {
        return fail((result.stderr || result.stdout || `artseek exited ${result.code}`).trim());
    }
    return { content: [{ type: 'text', text: result.stdout.trim() }] };
}

function send(message) {
    process.stdout.write(`${JSON.stringify(message)}\n`);
}

async function handle(message) {
    const { id, method, params } = message;
    if (method === 'initialize') {
        send({
            jsonrpc: '2.0',
            id,
            result: {
                protocolVersion: '2024-11-05',
                capabilities: { tools: {} },
                serverInfo: { name: 'artseek', version: '0.1.0' },
                instructions:
                    'Live, read-only ART heap inspection of the Koma debug build on a rooted arm64 device. ' +
                    'Start the app with `mise run run` first; Compose text is reachable through artseek_strsearch, not artseek_tree.',
            },
        });
        return;
    }
    if (method === 'notifications/initialized' || method === 'notifications/cancelled') return;
    if (method === 'ping') {
        send({ jsonrpc: '2.0', id, result: {} });
        return;
    }
    if (method === 'tools/list') {
        send({ jsonrpc: '2.0', id, result: { tools: TOOLS } });
        return;
    }
    if (method === 'tools/call') {
        const name = params?.name;
        const args = params?.arguments ?? {};
        try {
            const result = await callTool(name, args);
            send({ jsonrpc: '2.0', id, result });
        } catch (error) {
            send({ jsonrpc: '2.0', id, result: fail(error.message) });
        }
        return;
    }
    send({
        jsonrpc: '2.0',
        id,
        error: { code: -32601, message: `method not found: ${method}` },
    });
}

const lines = createInterface({ input: process.stdin, crlfDelay: Infinity });
lines.on('line', (line) => {
    if (!line.trim()) return;
    let message;
    try {
        message = JSON.parse(line);
    } catch {
        send({ jsonrpc: '2.0', id: null, error: { code: -32700, message: 'parse error' } });
        return;
    }
    handle(message).catch((error) => {
        process.stderr.write(`artseek-mcp: ${error.stack || error.message}\n`);
        if (message.id !== undefined) {
            send({ jsonrpc: '2.0', id: message.id, error: { code: -32603, message: error.message } });
        }
    });
});
