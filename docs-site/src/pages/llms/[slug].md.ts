import type {APIRoute, GetStaticPaths} from 'astro';
import {renderLlmsTemplate} from '../../data/llmsTemplate';

const slugs = [
    'cache',
    'datetime',
    'funktor',
    'karango',
    'kontainer',
    'kraft',
    'monko',
    'mutator',
    'slumber',
    'streams',
    'vault',
] as const;

export const prerender = true;

export const getStaticPaths: GetStaticPaths = () => slugs.map((slug) => ({params: {slug}}));

export const GET: APIRoute = ({params}) => {
    const slug = params.slug as string;
    return new Response(renderLlmsTemplate(`${slug}.md`), {
        headers: {'Content-Type': 'text/markdown; charset=utf-8'},
    });
};
