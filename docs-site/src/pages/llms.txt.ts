import type {APIRoute} from 'astro';
import {renderLlmsTemplate} from '../data/llmsTemplate';

export const prerender = true;

export const GET: APIRoute = () => {
    return new Response(renderLlmsTemplate('llms.txt'), {
        headers: {'Content-Type': 'text/plain; charset=utf-8'},
    });
};
