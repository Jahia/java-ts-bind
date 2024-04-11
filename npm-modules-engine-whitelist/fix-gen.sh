#!/bin/bash

# Directory containing the TypeScript definition files
DIR="build/types"

# Define arrays for search and replace pairs
searches=(
    "ServiceReference\\[\\]"
    "Map\\[\\]"
    "RangeIterator extends Iterator"
    ".*org\\.graalvm\\.polyglot.*;"
    "loadPropertiesResource(bundle: Bundle, path: string): ProxyObject;"
    "transformToJsNode(node: JCRNodeWrapper, includeChildren: boolean, includeDescendants: boolean, includeAllTranslations: boolean): ProxyObject;"
    "getRenderParameters(resource: Resource): ProxyObject;"
    "executeQuery(parameters: Map): Promise;"
    "executeQuerySync(parameters: Map): Value;"
    "getConfigPids(): ProxyArray;"
    "getConfigFactoryIdentifiers(factoryPid: string): ProxyArray;"
    "getConfigValues(configPid: string): ProxyObject;"
    "getConfigFactoryValues(factoryPid: string, factoryIdentifier: string): ProxyObject;"
    "import { Promise, ContextProvider } from 'org.jahia.modules.npm.modules.engine.jsengine';"
    "export class JCRNodeDecorator implements JCRNodeWrapper"
    "export class JCRSiteNode extends JCRNodeDecorator implements JahiaSite {"
    "export class JCRWorkspaceWrapper implements Workspace {"
    "renderComponent(attr: Map<string, any>, renderContext: RenderContext): string;"
    "render(attr: Map<string, any>, renderContext: RenderContext, currentResource: Resource): string;"
    "addResources(attr: Map<string, any>, renderContext: RenderContext): string;"
    "addCacheDependency(attr: Map<string, any>, renderContext: RenderContext): void;"
    "renderAbsoluteArea(attr: Map<string, any>, renderContext: RenderContext): string;"
    "renderArea(attr: Map<string, any>, renderContext: RenderContext): string;"
    "find(filter: Map<string, any>): any\\[\\];"
    "find(filter: Map<string, any>, orderBy: string): any\\[\\];"
)

replaces=(
    "ServiceReference<any>[]"
    "any[]"
    "RangeIterator extends Iterator<Item>"
    ""
    "loadPropertiesResource(bundle: Bundle, path: string): any;"
    "transformToJsNode(node: JCRNodeWrapper, includeChildren: boolean, includeDescendants: boolean, includeAllTranslations: boolean): any;"
    "getRenderParameters(resource: Resource): any;"
    "executeQuery(parameters: any): Promise<any>;"
    "executeQuerySync(parameters: any): any;"
    "getConfigPids(): string[];"
    "getConfigFactoryIdentifiers(factoryPid: string): string[];"
    "getConfigValues(configPid: string): any;"
    "getConfigFactoryValues(factoryPid: string, factoryIdentifier: string): any;"
    ""
    "export interface JCRNodeDecorator extends JCRNodeWrapper"
    "export interface JCRSiteNode extends JCRNodeDecorator,JahiaSite {"
    "export interface JCRWorkspaceWrapper extends Workspace {"
    "renderComponent(attr: any, renderContext: RenderContext): string;"
    "render(attr: any, renderContext: RenderContext, currentResource: Resource): string;"
    "addResources(attr: any, renderContext: RenderContext): string;"
    "addCacheDependency(attr: any, renderContext: RenderContext): void;"
    "renderAbsoluteArea(attr: any, renderContext: RenderContext): string;"
    "renderArea(attr: any, renderContext: RenderContext): string;"
    "find(filter: any): any[];"
    "find(filter: any, orderBy: string): any[];"
)

# Iterate over the arrays and perform the replacements
for i in "${!searches[@]}"; do
    find $DIR -name "*.d.ts" -exec sed -i '' -e "s/${searches[$i]}/${replaces[$i]}/g" {} \;
done
