package info.leadinglight.umljavadoclet.model;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ParameterizedType;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.Type;
import ext.plantuml.com.google.zxing.common.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a class internal or external to the model.
 */
public class ModelClass {
    public ModelClass(Model model, Type type, boolean isInternal, String comment) {
        _model = model;
        if(comment == null) {
            comment = " \n ";
        }
        comment = comment + " \n ";
        _comment = comment.trim().split("\n")[0];
        _type = type;
       _isInternal = isInternal;
        _classDoc = _type.asClassDoc();
    }
    
    public enum Visibility {
        PUBLIC, PROTECTED, PRIVATE, PACKAGE_PRIVATE
    }
    
    public enum ClassType {
        INTERFACE, ENUM, CLASS, ABSTRACT
    }
    
    public static class VisibilityItem {
        public VisibilityItem(Visibility visibility) {
            this.visibility = visibility;
        }
        
        public Visibility visibility;
    }
    
    public static class Field extends VisibilityItem {
//        public Field(String name, String type, Visibility visibility, boolean isStatic) {
//            super(visibility);
//            this.name = name;
//            this.type = type;
//            this.isStatic = isStatic;
//        }
        
        public String name;
        public String type;
        public boolean isStatic;

        public String rawCommentDoc;
        public Field(String name, String type, Visibility visibility, boolean isStatic, String rawCommentDoc) {
            super(visibility);
            this.name = name;
            this.type = type;
            this.isStatic = isStatic;
            System.out.println("get print raw comment: " + rawCommentDoc);
            if(rawCommentDoc == null) {
                rawCommentDoc = " \n ";
            }
            rawCommentDoc = rawCommentDoc + " \n ";
            this.rawCommentDoc = rawCommentDoc.trim().split("\n")[0];
        }
    }
    
    public static class Constructor extends VisibilityItem {
        public Constructor(String name, List<MethodParameter> parameters, Visibility visibility) {
            super(visibility);
            this.name = name;
            this.parameters = parameters;
        }
        
        public String name;
        public List<MethodParameter> parameters;
    }
    
    public static class Method extends VisibilityItem {
        public Method(String name, List<MethodParameter> parameters, String returnType, Visibility visibility, boolean isAbstract, boolean isStatic) {
            super(visibility);
            this.name = name;
            this.parameters = parameters;
            this.returnType = returnType;
            this.isAbstract = isAbstract;
            this.isStatic = isStatic;
        }
        
        public String name;
        public List<MethodParameter> parameters;
        public String returnType;
        public boolean isAbstract;
        public boolean isStatic;
    }
    
    public static class MethodParameter {
        public MethodParameter(String type, String name) {
            this.type = type;
            this.name = name;
        }
        
        public String type;
        public String name;
    }
    
    public String fullName() {
        return fullName(_type);
    }
    
    public String fullNameWithoutParameters() {
        return fullNameWithoutParameters(_type);
    }
    public String comment() {
        return _comment;
    }

    public String shortName() {
        return shortName(_type);
    }
    
    public String shortNameWithoutParameters() {
        return shortNameWithoutParameters(_type);
    }
    
    public String packageName() {
        return _classDoc.containingPackage().name();
    }
    
    public static String fullName(Type type) {
        // The full name of the class is not used for display purposes,
        // but rather to uniquely identify the class across all packages.
        // This needs to include the parameters of the class.
        return type.toString();
    }

    public static String shortName(Type type) {
        // The short name of the class is used for display purposes.
        // This can be for the name of the class, an attribute, or a method parameter / return type.
        // The logic for determining the name of a class is very complicated,
        // and needs to take into account parameterized types, type variables, etc.
        // There is no clean way to do that from the Javadoc classes.
        // The hack is to use the result of the toString() method and massage it.
        String fullName = type.toString();
        String shortName = getShortName(fullName);
        return shortName;
    }

    private static String getShortName(String fullName) {
        // The toString() contains package identifiers for the classes that are referenced.
        // We need to remove all of the package identifiers.
        // We need to do this recursively on any generics, since they can be embedded.
        int paramIndex = fullName.indexOf('<');

        String shortName = paramIndex != -1
            ? stripQualifier(fullName.substring(0, paramIndex))
            : stripQualifier(fullName);

        // Parameter declaration: clean up all the parameters.
        if (paramIndex != -1) {
            String paramStr = fullName.substring(paramIndex + 1);
            String[] parts = paramStr.split(" ");
            String shortParamStr = "";
            for (String part : parts) {
                shortParamStr += getShortName(part) + " ";
            }
            shortName += "<" + shortParamStr;
        }
        return shortName;
    }

    private static String stripQualifier(String name) {
        String[] parts = name.split("\\.");
        int index = 0;
        for (String part: parts) {
            // We assume that an uppercase character indicates the name of a class.
            if (Character.isUpperCase(part.charAt(0))) {
                break;
            }
            index++;
        }

        // Assemble the remaining parts.
        String result = "";
        for (int i = index; i < parts.length - 1; i++) {
            result += parts[i] + ".";
        }
        result += parts[parts.length - 1];

        return result;
    }

    public static String fullNameWithoutParameters(Type type) {
        String fullName = "";
        ClassDoc classDoc = type.asClassDoc();
        if (classDoc != null) {
            fullName = classDoc.containingPackage().name() + "." + shortNameWithoutParameters(type);
        } else {
            fullName = type.qualifiedTypeName();
        }
        return fullName;
    }

    public static String shortNameWithoutParameters(Type type) {
        ClassDoc classDoc = type.asClassDoc();
        if (classDoc != null) {
            // If this is an inner class, put the name of the enclosing class
            // as the first part of this class' short name.
            if (isInnerClass(classDoc)) {
                return classDoc.containingClass().simpleTypeName() + "." + type.simpleTypeName();
            } else {
                return classDoc.simpleTypeName();
            }
        } else {
            return type.simpleTypeName();
        }
    }

    public ClassType type() {
        if (_classDoc.isInterface()) {
            return ClassType.INTERFACE;
        } else if (_classDoc.isEnum()) {
            return ClassType.ENUM;
        } else if (_classDoc.isAbstract()) {
            return ClassType.ABSTRACT;
        } else {
            return ClassType.CLASS;
        }
    }
    
    public List<String> annotations() {
        List<String> annotations = new ArrayList<>();
        for (AnnotationDesc annotation: _classDoc.annotations()) {
            annotations.add(annotation.annotationType().simpleTypeName());
        }
        return annotations;
    }
    
    public boolean isInternal() {
        return _isInternal;
    }
    
    public boolean isExternal() {
        return !isInternal();
    }
    
    public boolean isParameterized() {
        return _type.asParameterizedType() != null;
    }
    
    public boolean isInnerClass() {
        return isInnerClass(_classDoc);
    }
    
    public static boolean isInnerClass(ClassDoc classDoc) {
        return classDoc.containingClass() != null;
    }
    
    public List<String> parameters() {
        return buildParameters(_type);
    }

    private static List<String> buildParameters(Type type) {
        List<String> params = new ArrayList<>();
        ParameterizedType paramType = type.asParameterizedType();
        if (paramType != null) {
            for (Type param : paramType.typeArguments()) {
                String name = ModelClass.shortName(param);
                params.add(name);
            }
        }
        return params;
    }

    public List<ModelClass> parameterClasses() {
        return _params;
    }
    
    public boolean isCollectionClass() {
        return _type.qualifiedTypeName().equals("java.util.List") || _type.qualifiedTypeName().equals("java.util.Map");
    }
    
    public ModelPackage modelPackage() {
        String packageName = _classDoc.containingPackage().name();
        ModelPackage modelPackage = _model.modelPackage(packageName);
        return modelPackage;
    }
    
    public List<ModelRel> relationships() {
        return _rels;
    }
    
    public RelFilter relationshipsFilter() {
        return new RelFilter(_rels);
    }
    
    public ModelClass superclass() {
        ModelRel rel = relationshipsFilter().source(this).kind(ModelRel.Kind.GENERALIZATION).first();
        return rel != null ? rel.destination() : null;
    }
    
    public List<ModelClass> interfaces() {
        return relationshipsFilter().source(this).kind(ModelRel.Kind.REALIZATION).destinationClasses();
    }
    
    public List<ModelRel> sourceAssociations() {
        return relationshipsFilter().source(this).kind(ModelRel.Kind.DIRECTED_ASSOCIATION).all();
    }
    
    public List<ModelRel> destinationAssociations() {
        return relationshipsFilter().destination(this).kind(ModelRel.Kind.DIRECTED_ASSOCIATION).all();
    }

    public List<ModelClass> dependencies() {
        return relationshipsFilter().source(this).kind(ModelRel.Kind.DEPENDENCY).destinationClasses();
    }
    
    public List<ModelClass> dependents() {
        return relationshipsFilter().destination(this).kind(ModelRel.Kind.DEPENDENCY).sourceClasses();
    }
    
    public boolean hasRelationshipWith(ModelClass dest) {
        return relationshipsFilter().source(this).destination(dest).first() != null;
    }
    
    public ModelRel dependencyWith(ModelClass dest) {
        return relationshipsFilter().source(this).destination(dest).kind(ModelRel.Kind.DEPENDENCY).first();
    }

    public boolean hasDependencyWith(ModelClass dest) {
        return dependencyWith(dest) != null;
    }
    
    public List<Field> fields() {
        return _fields;
    }
    
    public List<Constructor> constructors() {
        return _constructors;
    }
    
    public List<Method> methods() {
        return _methods;
    }
    
    // Update Model
    
    public void addRelationship(ModelRel rel) {
        _rels.add(rel);
    }
    
    // Mapping

    public void map() {
        mapParameters();
        if (isInternal()) {
            // Only map internal classes.
            mapInternals();
            mapRelationships();
        }
    }

    private void mapInternals() {
        mapFields();
        mapConstructors();
        mapMethods();
    }
    
    private void mapRelationships() {
        // Map field associations first, since that will establish the has relationships, which are stronger
        // than any of the dependency relationships that may follow.
        mapFieldAssociations();
        mapSuperclass();
        mapInterfaces();
        mapConstructorDependencies();
        mapMethodDependencies();
    }
    
    private void mapParameters() {
        ParameterizedType paramType = _type.asParameterizedType();
        if (paramType != null) {
            for (Type type: paramType.typeArguments()) {
                String typeName = type.qualifiedTypeName();
                if (!typeName.startsWith("java.lang.") && !type.isPrimitive()) {
                    ModelClass param = _model.createClassIfNotExists(type);
                    _params.add(param);
                }
            }
        }
    }
    
    private void mapSuperclass() {
        Type superclassType = _classDoc.superclassType();
        if (superclassType != null) {
            String superclassName = superclassType.qualifiedTypeName();
            // Do not include standard Java superclasses in the model.
            if (!superclassName.equals("java.lang.Object") && !superclassName.equals("java.lang.Enum")) {
                ModelClass dest = _model.createClassIfNotExists(superclassType);
                ModelRel rel = new ModelRel(ModelRel.Kind.GENERALIZATION, this, dest);
                mapSourceRel(rel);
                mapParamDependencies(dest);
            }
        }
    }
    
    private void mapInterfaces() {
        for (Type interfaceType: _classDoc.interfaceTypes()) {
            ModelClass dest = _model.createClassIfNotExists(interfaceType);
            // If source class is an interface, than the relationship is a generalization, not a realization.
            ModelRel.Kind kind = _classDoc.isInterface() ? ModelRel.Kind.GENERALIZATION : ModelRel.Kind.REALIZATION;
            ModelRel rel = new ModelRel(kind, this, dest);
            mapSourceRel(rel);
            mapParamDependencies(dest);
        }        
    }
    
    private void mapFieldAssociations() {
        for (FieldDoc fieldDoc: _classDoc.fields(false)) {
            Type type = fieldDoc.type();
            String typeName = type.qualifiedTypeName();
            // TODO Relationships through collection types.
            if (!type.simpleTypeName().equals("void") && !typeName.startsWith("java.lang.") && !type.isPrimitive()) {
                ModelClass dest = _model.createClassIfNotExists(type);
                ModelRel rel = new ModelRel(ModelRel.Kind.DIRECTED_ASSOCIATION, this, dest, fieldDoc.name());
                mapSourceRel(rel);
                mapParamAssociations(fieldDoc, dest);
                mapParamDependencies(dest);
            }
        }
    }
    
    private void mapParamAssociations(FieldDoc fieldDoc, ModelClass modelClass) {
        // If the modelclass is a parameterized collection, then we want to model a 1..many relationship with the collection.
        if (modelClass.isParameterized() && modelClass.isCollectionClass()) {
            for (ModelClass param: modelClass.parameterClasses()) {
                ModelRel rel = new ModelRel(ModelRel.Kind.DIRECTED_ASSOCIATION, this, param, fieldDoc.name(), ModelRel.Multiplicity.MANY);
                mapSourceRel(rel);
            }
        }
    }
    
    private void mapConstructorDependencies() {
        for (ConstructorDoc constructorDoc: _classDoc.constructors(false)) {
            for (Parameter param: constructorDoc.parameters()) {
                Type type = param.type();
                mapTypeDependency(type, constructorDoc.isPublic(), constructorDoc.isProtected(), constructorDoc.isPackagePrivate(), constructorDoc.isPrivate());
            }
        }
    }

    private void mapMethodDependencies() {
        for (MethodDoc methodDoc: _classDoc.methods(false)) {
            for (Parameter param: methodDoc.parameters()) {
                Type type = param.type();
                mapTypeDependency(type, methodDoc.isPublic(), methodDoc.isProtected(), methodDoc.isPackagePrivate(), methodDoc.isPrivate());
            }
            Type returnType = methodDoc.returnType();
            mapTypeDependency(returnType, methodDoc.isPublic(), methodDoc.isProtected(), methodDoc.isPackagePrivate(), methodDoc.isPrivate());
        }
    }
    
    private void mapTypeDependency(Type type, boolean isPublic, boolean isProtected, boolean isPackage, boolean isPrivate) {
        String typeName = type.qualifiedTypeName();
        // TODO Relationships through collection types.
        if (!type.simpleTypeName().equals("void") && !typeName.startsWith("java.lang.") && !type.isPrimitive()) {
            ModelClass dest = _model.createClassIfNotExists(type);

            ModelRel.Visibility visibility = null;
            if (isPublic) {
                visibility = ModelRel.Visibility.PUBLIC;
            } else if (isProtected) {
                visibility = ModelRel.Visibility.PROTECTED;
            } else if (isPackage) {
                visibility = ModelRel.Visibility.PACKAGE;
            } else if (isPrivate) {
                visibility = ModelRel.Visibility.PRIVATE;
            }

            // If there is already a dependency with the other class, and it has a visibility
            // weaker than this visibility, than replace it with this visibility.
            if (this != dest && visibility != null) {
                if (hasDependencyWith(dest)) {
                    ModelRel dependencyWith = dependencyWith(dest);
                    if (visibility == ModelRel.Visibility.PUBLIC) {
                        dependencyWith.changeVisibility(visibility);
                    } else if (visibility == ModelRel.Visibility.PROTECTED) {
                        if (dependencyWith.destinationVisibility() != ModelRel.Visibility.PUBLIC) {
                            dependencyWith.changeVisibility(visibility);
                        }
                    } else if (visibility == ModelRel.Visibility.PACKAGE) {
                        if (dependencyWith.destinationVisibility() != ModelRel.Visibility.PUBLIC &&
                                dependencyWith.destinationVisibility() != ModelRel.Visibility.PROTECTED) {
                            dependencyWith.changeVisibility(visibility);
                        }
                    }
                }
            }
            
            // Only add if there is no existing relationship with the class.
            // Do not add dependency to this class.
            if (this != dest && !hasRelationshipWith(dest)) {
                ModelRel rel = new ModelRel(ModelRel.Kind.DEPENDENCY, this, dest, visibility);
                mapSourceRel(rel);
            }

            mapParamDependencies(dest);
        }
    }
    
    private void mapParamDependencies(ModelClass modelClass) {
        // Is the destination class a parameterized class? If so, there is a dependency on the underlying parameters.
        if (modelClass.isParameterized()) {
            for (ModelClass param: modelClass.parameterClasses()) {
                // In some cases, the generic parameter can be returned as a '?', instead of the actual class.
                // I think this can happen with lists to inner classes (that happen in this class, for example).
                // Filter them out.
                if (!param._type.toString().startsWith("? ")) {
                    // Do not map a dependency relationship back to this class.
                    if (!param._type.equals(_type)) {
                        // Only map the dependency if there is no existing relationship with that class.
                        if (!hasRelationshipWith(param)) {
                            ModelRel paramRel = new ModelRel(ModelRel.Kind.DEPENDENCY, this, param);
                            mapSourceRel(paramRel);
                        }
                    }
                }
                // Relationships through embedded parameter types:
                // If the dependent class is also parameterized, expand relationships through the parameters.
                mapParamDependencies(param);
            }
        }
    }

    private void mapFields() {
        List<Field> fields = new ArrayList<>();
        for (FieldDoc fieldDoc: _classDoc.fields(false)) {
            Type type = fieldDoc.type();
            String typeName = shortName(type);
            Field mappedField = new Field(fieldDoc.name(), typeName, mapVisibility(fieldDoc), fieldDoc.isStatic(), fieldDoc.getRawCommentText());
            fields.add(mappedField);
        }
        orderVisibility(fields, _fields);
    }
    
    private void mapConstructors() {
        List<Constructor> constructors = new ArrayList<>();
        for (ConstructorDoc consDoc: _classDoc.constructors(false)) {
            List<MethodParameter> params = new ArrayList<>();
            for (Parameter param: consDoc.parameters()) {
                Type paramType = param.type();
                String paramTypeName = shortName(paramType);
                params.add(new MethodParameter(paramTypeName, param.name()));
            }
            Constructor constructor = new Constructor(consDoc.name(), params, mapVisibility(consDoc));
            constructors.add(constructor);
        }
        orderVisibility(constructors, _constructors);
    }
    
    private void mapMethods() {
        List<Method> methods = new ArrayList<>();
        for (MethodDoc methodDoc: _classDoc.methods(false)) {
            List<MethodParameter> params = new ArrayList<>();
            for (Parameter param: methodDoc.parameters()) {
                Type paramType = param.type();
                String paramTypeName = shortName(paramType);
                params.add(new MethodParameter(paramTypeName, param.name()));
            }
            Type returnType = methodDoc.returnType();
            String returnTypeName = shortName(returnType);
            Method method = new Method(methodDoc.name(), 
                    params, 
                    returnTypeName,
                    mapVisibility(methodDoc),
                    methodDoc.isAbstract(),
                    methodDoc.isStatic());
            methods.add(method);
        }
        orderVisibility(methods, _methods);
    }

    private void mapSourceRel(ModelRel rel) {
        _rels.add(rel);
        // Do not add relationships back to ourselves more than once.
        ModelClass dest = rel.destination();
        if (this != dest) {
            dest.addRelationship(rel);
        }
    }
    
    private Visibility mapVisibility(ProgramElementDoc doc) {
        if (doc.isPublic()) {
            return Visibility.PUBLIC;
        } else if(doc.isProtected()) {
            return Visibility.PROTECTED;
        } else if(doc.isPrivate()) {
            return Visibility.PRIVATE;
        } else {
            return Visibility.PACKAGE_PRIVATE;
        }
    }
    
    private void orderVisibility(List<? extends VisibilityItem> items, List<? extends VisibilityItem> filteredItems) {
        filterVisibility(items, filteredItems, Visibility.PUBLIC);
        filterVisibility(items, filteredItems, Visibility.PROTECTED);
        filterVisibility(items, filteredItems, Visibility.PACKAGE_PRIVATE);
        filterVisibility(items, filteredItems, Visibility.PRIVATE);
    }
    
    private void filterVisibility(List<? extends VisibilityItem> items, List<? extends VisibilityItem> filteredItems, Visibility visibility) {
        for (VisibilityItem item: items) {
            if (item.visibility == visibility) {
                ((List<VisibilityItem>)filteredItems).add(item);
            }
        }
    }
    
    private final Model _model;
    private final String _comment;
    private final Type _type;
    private final ClassDoc _classDoc;
    private final List<ModelClass> _params = new ArrayList<>();
    private final List<ModelRel> _rels = new ArrayList<>();
    private final List<Field> _fields = new ArrayList<>();
    private final List<Constructor> _constructors = new ArrayList<>();
    private final List<Method> _methods = new ArrayList<>();
    
    private final boolean _isInternal;
}
